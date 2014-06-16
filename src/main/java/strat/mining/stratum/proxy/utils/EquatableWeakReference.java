/**
 * stratum-proxy is a proxy supporting the crypto-currency stratum pool mining
 * protocol.
 * Copyright (C) 2014  Stratehm (stratehm@hotmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with multipool-stats-backend. If not, see <http://www.gnu.org/licenses/>.
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