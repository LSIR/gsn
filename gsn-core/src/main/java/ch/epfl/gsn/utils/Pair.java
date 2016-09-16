package ch.epfl.gsn.utils;

public class Pair<T,U> {
	public T getFirst() {
		return first;
	}

	public void setFisrt(T first) {
		this.first = first;
	}

	public U getSecond() {
		return second;
	}

	public void setSecond(U second) {
		this.second = second;
	}

	T first;
	U second;
	
	public Pair(T t, U u){
		this.first = t;
		this.second = u;
	}

}
