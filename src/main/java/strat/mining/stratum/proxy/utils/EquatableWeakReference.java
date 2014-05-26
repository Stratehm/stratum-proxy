/*
 * Copyright (c) 2006-2008 Chris Smith, Shane Mc Cormack, Gregory Holmes
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package strat.mining.stratum.proxy.utils;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

/**
 * An extension of WeakReference that implements a sane equals and hashcode
 * method.
 * 
 * @param <T>
 *            The type of object that this reference contains
 * @author chris
 */
public class EquatableWeakReference<T> extends WeakReference<T> {

	/**
	 * Creates a new instance of EquatableWeakReference.
	 * 
	 * @param referent
	 *            The object that this weak reference should reference.
	 */
	public EquatableWeakReference(T referent) {
		super(referent);
	}

	/** {@inheritDoc} */
	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(Object obj) {
		if (get() == null) {
			super.equals(obj);
		}
		if (obj instanceof Reference) {
			return get().equals(((Reference) obj).get());
		} else {
			return get().equals(obj);
		}
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		if (get() == null) {
			return super.hashCode();
		}
		return get().hashCode();
	}

}