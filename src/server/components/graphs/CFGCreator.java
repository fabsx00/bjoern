package server.components.graphs;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

import server.Constants;

public class CFGCreator
{

	protected OrientGraphNoTx g;

	public CFGCreator(OrientGraphNoTx graph)
	{
		this.g = graph;
	}

	public Graph createCFG(Vertex functionNode)
	{
		String id = functionNode.getId().toString();
		Long functionId = Long.parseLong(id.split(":")[1]);
		Graph sg = new TinkerGraph();
		Iterable<Vertex> basicBlocks = getBasicBlocksOfFunction(functionId);
		// Connect basic blocks with instructions
		for (Vertex bb : basicBlocks)
		{
			Vertex v = sg.addVertex(bb.getId());
			for (String property : bb.getPropertyKeys())
			{
				v.setProperty(property, bb.getProperty(property));
			}
			for (Edge edge : bb.getEdges(Direction.OUT, "IS_BB_OF"))
			{
				Vertex instr = edge.getVertex(Direction.IN);
				Vertex w = sg.addVertex(instr);
				for (String property : instr.getPropertyKeys())
				{
					w.setProperty(property, instr.getProperty(property));
				}
				Edge e = sg.addEdge(edge.getId(), v, w, edge.getLabel());
				for (String property : edge.getPropertyKeys())
				{
					e.setProperty(property, edge.getProperty(property));
				}
			}
		}
		// Connect basic blocks with each other
		for (Vertex bb : basicBlocks)
		{
			for (Edge edge : bb.getEdges(Direction.OUT, "CFLOW_ALWAYS",
					"CFLOW_TRUE", "CFLOW_FALSE"))
			{
				Vertex v = sg.getVertex(bb.getId());
				Vertex w = sg.getVertex(edge.getVertex(Direction.IN));
				if (w != null)
				{
					Edge e = sg.addEdge(edge.getId(), v, w, edge.getLabel());
					for (String property : edge.getPropertyKeys())
					{
						e.setProperty(property, edge.getProperty(property));
					}
				}
			}
		}
		return sg;
	}

	protected Iterable<Vertex> getBasicBlocksOfFunction(Long functionId)
	{
		return g.getVertices("V", Constants.INDEX_KEYS,
				new String[] { "functionId:" + functionId, "nodeType:BB" });
	}
}
