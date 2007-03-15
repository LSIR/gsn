package gsn.utils.graph;

public class Edge<T> {
	private Node<T> startNode;

	private Node<T> endNode;

	public Edge(Node<T> startNode, Node<T> endNode) {
		this.startNode = startNode;
		this.endNode = endNode;
	}

	public Node<T> getEndNode() {
		return endNode;
	}

	public void setEndNode(Node<T> endNode) {
		this.endNode = endNode;
	}

	public Node<T> getStartNode() {
		return startNode;
	}

	public void setStartNode(Node<T> startNode) {
		this.startNode = startNode;
	}

}
