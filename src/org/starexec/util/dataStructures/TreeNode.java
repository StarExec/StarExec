package org.starexec.util.dataStructures;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class TreeNode<T> implements Iterable<TreeNode<T>> {
	private T data;
	private TreeNode<T> parent;
	private List<TreeNode<T>> children;

	/**
	 * Basic constructor that makes a new TreeNode.
	 * @param nodeData the data to be contained in the TreeNode
	 * @author Albert Giegerich
	 */
	public TreeNode(T nodeData) {
		data = nodeData;
		parent = null;
		children = new ArrayList<>();
	}

	/**
	 * @return The data in the TreeNode.
	 */
	public T getData() {
		return data;
	}

	/**
	 * @return the parent of the TreeNode
	 */
	public TreeNode<T> getParent() {
		return parent;
	}

	/**
	 * @return the number of children that the TreeNode has
	 * @author Albert Giegerich
	 */
	public int getNumberOfChildren() {
		return children.size();
	}

	/**
	 * @param indexOfchildToGet the index of the child in this TreeNode's list to get
	 * @return the child at indexOfChildToGet
	 * @author Albert Giegerich
	 */
	public TreeNode<T> getChild(int indexOfChildToGet) {
		try {
			return children.get(indexOfChildToGet);
		} catch (IndexOutOfBoundsException e) {
			throw new IndexOutOfBoundsException("The TreeNode has less than "+(indexOfChildToGet+1)+" children.");
		}
	}

	/**
	 * @param child the child to find and index for
	 * @return the index of the input child
	 * @author Albert Giegerich
	 */
	public int getIndexOfChild(TreeNode<T> child) {
		return children.indexOf(child);
	}

	/**
	 * Adds a child to the list of children.
	 * @param newChild the child to be added to the list of children.
	 * @throws NullPointerException if the new child is null.
	 * @author Albert Giegerich
	 */
	public void addChild(TreeNode<T> newChild) {
		if (newChild == null) {
			throw new NullPointerException("The new child to add cannot be null.");
		} else if (this.hasChild(newChild)) {
			return;
		} else {
			children.add(newChild);
			newChild.setParent(this);
		}
	}

	/**
	 * Removes a child from the list of children.
	 * @param childToDelete the child to be deleted from the list of children
	 * @author Albert Giegerich
	 */
	public boolean removeChild(TreeNode<T> childToDelete) {
		boolean childRemoved  = children.remove(childToDelete);
		childToDelete.setParent(null);
		return childRemoved;
	}


	/**
	 * Deletes a child given it's index.
	 * @param indexOfChildToDelete the index of the child in the list of children to delete
	 * @return the child that was deleted
	 * @throws IndexOutOfBoundsException if the index of the child to be deleted is out of bounds.
	 * @author Albert Giegerich
	 */
	public TreeNode<T> removeChild(int indexOfChildToDelete) {
		try {
			TreeNode<T> child = children.remove(indexOfChildToDelete);
			child.setParent(null);
			return child;
		} catch (IndexOutOfBoundsException e) {
			throw new IndexOutOfBoundsException("The TreeNode has less than "+(indexOfChildToDelete+1)+" children.");
		}
	}

	
	/**
	 * Checks if the input TreeNode is a child of this TreeNode
	 * @param possibleChild the TreeNode to test if it is a child.
	 * @return true if possibleChild is a child of this TreeNode
	 */
	public boolean hasChild(TreeNode<T> possibleChild) {
		if (possibleChild == null) {
			return false;
		}
		return children.contains(possibleChild);
	}

	@Override
	public Iterator<TreeNode<T>> iterator() {
		Iterator<TreeNode<T>> iterator = new Iterator<TreeNode<T>>() {
			private int currentIndex = 0;
			
			@Override
			public boolean hasNext() {
				return currentIndex < getNumberOfChildren();
			}

			@Override
			public TreeNode<T> next() {
				TreeNode<T> child = children.get(currentIndex);
				currentIndex += 1;
				return child;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
		return iterator;
	}

	/**
	 * Sets the parent of this TreeNode.
	 * @param newParent the new parent of this TreeNode
	 */
	private void setParent(TreeNode<T> newParent) {
		parent = newParent;
		if (newParent != null) {
			newParent.addChild(this);
		}
	}

	
}
