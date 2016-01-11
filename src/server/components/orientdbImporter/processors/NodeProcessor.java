package server.components.orientdbImporter.processors;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import com.opencsv.CSVReader;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientVertexType;
import com.tinkerpop.blueprints.util.wrappers.batch.BatchGraph;

import server.Constants;
import server.components.orientdbImporter.CSVImporter;

public class NodeProcessor extends CSVFileProcessor
{

	public NodeProcessor(CSVImporter importer)
	{
		super(importer);
	}

	@Override
	protected void processFirstRow(CSVReader csvReader, String[] row)
			throws IOException
	{

		initializeVertexKeys(row);
		createPropertiesAndIndices();

	}

	private void initializeVertexKeys(String[] row)
	{
		String[] keys = rowToKeys(row);
		importer.setVertexKeys(keys);
	}

	private void createPropertiesAndIndices()
	{

		if (!importer.isNewDatabase())
			return;

		OrientVertexType vType = importer.getNoTx().getVertexType("V");

		for (String key : importer.getVertexKeys())
		{
			vType.createProperty(key, OType.STRING);
		}

		List<String> keysToIndex = new LinkedList<String>();
		for (String key : importer.getVertexKeys())
		{
			keysToIndex.add(key);
		}

		String[] indexKeys = new String[keysToIndex.size()];
		keysToIndex.sort(null);
		keysToIndex.toArray(indexKeys);

		vType.createIndex("nodeIndex.", "FULLTEXT", null, null, "LUCENE",
				indexKeys);
	}

	@Override
	protected void processRow(String[] row)
	{
		// skip empty lines
		if (row.length < 1)
			return;

		String id = row[0];

		String[] properties = new String[2 * row.length];
		for (int i = 0; i < row.length; i++)
		{
			properties[2 *i] = importer.getVertexKeys()[i];
			properties[2 *i + 1] = row[i];
		}
		Object[] props = properties;
		createNodeInGraph(id, props);
	}

	private void createNodeInGraph(String id, Object[] props)
	{
		doCreateNodeInGraph(id, props, 0);
	}

	private void doCreateNodeInGraph(String baseId, Object[] props, int num)
	{
		BatchGraph<?> batchGraph = (BatchGraph<?>) importer.getGraph();

		if(num == Constants.MAX_NODES_FOR_KEY)
			throw new RuntimeException("Too many nodes with the same key: " + baseId);

		// The first node gets the baseId, all others will
		// obtain an additional "_$number" and will be connected to
		// the last alternative.

		String completeId = createCompleteId(baseId, num);

		try {
			batchGraph.addVertex(completeId, props);

			if(num != 0){
				linkToPreviousNode(baseId, num);
			}

		} catch (IllegalArgumentException e) {
			doCreateNodeInGraph(baseId, props, num + 1);
		}
	}

	private String createCompleteId(String baseId, int num)
	{
		String completeId;
		if(num == 0)
			completeId = baseId;
		else
			completeId = String.format("%s_%d", baseId, num);
		return completeId;
	}

	private void linkToPreviousNode(String baseId, int num)
	{
		String previousId = createCompleteId(baseId, num -1);
		String thisId = createCompleteId(baseId, num);

		Graph graph = importer.getGraph();

		Vertex fromNode = graph.getVertex(previousId);
		Vertex toNode = graph.getVertex(thisId);

		graph.addEdge(0, fromNode, toNode, "foo");
	}

}
