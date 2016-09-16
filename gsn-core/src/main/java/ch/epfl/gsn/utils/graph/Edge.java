/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2016, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* GSN is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with GSN.  If not, see <http://www.gnu.org/licenses/>.
* 
* File: src/ch/epfl/gsn/utils/graph/Edge.java
*
* @author Mehdi Riahi
* @author Timotee Maret
*
*/

package ch.epfl.gsn.utils.graph;

import java.io.Serializable;

public class Edge<T> implements Serializable{

	private static final long serialVersionUID = -8165242353963312649L;

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
