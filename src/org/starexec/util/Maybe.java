package org.starexec.util;

/** This class is designed to help us return either a value or else an error message,
    from methods that might fail. */
    
public class Maybe<T> {
    public T value;
    public String error;

    public Maybe(T val) {
	value = val;
	error = null;
    }

    public Maybe(String err) {
	value = null;
	error = err;
    }

    public <S> Maybe(Maybe<S> m) {
	value = null;
	error = m.error;
    }

    public void setError(String err) {
	error = err;
    }

    public boolean hasError() {
	return error != null;
    }
};