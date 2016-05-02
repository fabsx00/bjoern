package bjoern.plugins.alocs;

import java.io.IOException;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;

import bjoern.pluginlib.LookupOperations;
import bjoern.pluginlib.plugintypes.RadareProjectPlugin;

public class AlocPlugin extends RadareProjectPlugin {

	OrientGraphNoTx graph;

	@Override
	public void execute() throws Exception
	{
		graph = orientConnector.getNoTxGraphInstance();
		Iterable<Vertex> allFunctions = LookupOperations.getAllFunctions(graph);

		createAlocsForFunctions(allFunctions);

		graph.shutdown();
	}

	private void createAlocsForFunctions(Iterable<Vertex> functions) throws IOException
	{
		for(Vertex func : functions)
		{
			new FunctionAlocCreator(radare, graph).createAlocsForFunction(func);
		}

	}

}
