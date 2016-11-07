package octopus.server.components.gremlinShell;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import octopus.server.components.gremlinShell.io.BjoernClientReader;
import octopus.server.components.gremlinShell.io.BjoernClientWriter;
import octopus.server.components.shellmanager.ShellManager;

public class ShellRunnable implements Runnable
{
	private static final Logger logger = LoggerFactory
			.getLogger(ShellRunnable.class);

	private ServerSocket serverSocket;
	private OctopusGremlinShell shell;
	private Socket clientSocket;
	private BjoernClientWriter clientWriter;
	private BjoernClientReader clientReader;

	private boolean listening = true;

	public ShellRunnable(OctopusGremlinShell shell) throws IOException
	{
		this.shell = shell;
		createLocalListeningSocket();
	}

	@Override
	public void run()
	{
		shell.activateGraphOnCurrentThread();
		try
		{
			processClients();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}


	private void createLocalListeningSocket() throws IOException
	{
		InetAddress bindAddr = InetAddress.getLoopbackAddress();
		serverSocket = new ServerSocket(shell.getPort(), 10,
				bindAddr);
	}

	private void processClients() throws IOException
	{
		while (listening)
		{
			try
			{
				acceptNewClient();
				closeListeningSocket();

				markShellAsOccupied();
				handleClient();
				markShellAsFree();

				createLocalListeningSocket();
			} catch (IOException e)
			{
				logger.warn("IOException when handling client: {}",
						e.getMessage());
			}

		}
		ShellManager.destroyShell(shell.getPort());
		serverSocket.close();
	}

	private void markShellAsOccupied()
	{
		shell.markAsOccupied();
	}

	private void markShellAsFree()
	{
		shell.markAsFree();
	}


	private void closeListeningSocket() throws IOException
	{
		serverSocket.close();
	}

	private void acceptNewClient() throws IOException
	{
		clientSocket = serverSocket.accept();
		initClientWriter();
		initClientReader();
	}

	private void initClientReader() throws IOException
	{
		InputStream in = clientSocket.getInputStream();
		clientReader = new BjoernClientReader(new InputStreamReader(in));
	}

	private void initClientWriter() throws IOException
	{
		OutputStream out = clientSocket.getOutputStream();
		clientWriter = new BjoernClientWriter(new OutputStreamWriter(out));
	}

	private void handleClient() throws IOException
	{

		String message;
		while ((message = clientReader.readMessage()) != null)
		{
			if (message.equals("quit"))
			{
				listening = false;
				clientWriter.writeMessage("bye");
				shell.shutdownGraph();
				break;
			} else
			{
				Object evalResult;
				try
				{
					evalResult = shell.execute(message);
					clientWriter.writeResult(evalResult);
				} catch (Exception ex)
				{
					StringWriter strWriter = new StringWriter();
					PrintWriter printWriter = new PrintWriter(strWriter);
					ex.printStackTrace(printWriter);
					String errorMessage = ex.getMessage() + "\n" + strWriter;
					if(errorMessage != null)
						clientWriter.writeMessage(errorMessage);
					else
						clientWriter.writeMessage(ex.toString());
				}
			}

		}
		clientSocket.close();
	}
}
