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

import java.math.BigInteger;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;

//  
// AtomicBigInteger -- Mutable implementation of BigInteger, implemented  
//      with the concurrent atomic package.  
public class AtomicBigInteger extends Number implements Comparable<Object> {
	private static final long serialVersionUID = -94825735363453200L;
	private AtomicReference<BigInteger> value;

	// Constructors
	public AtomicBigInteger() {
		this(new byte[] { 0 });
	}

	public AtomicBigInteger(byte[] initVal) {
		this(new BigInteger(initVal));
	}

	public AtomicBigInteger(BigInteger initVal) {
		value = new AtomicReference<BigInteger>(initVal);
	}

	public AtomicBigInteger(Object initVal) {
		this(objectToBigInteger(initVal));
	}

	// Atomic methods
	public BigInteger get() {
		return value.get();
	}

	public void set(BigInteger newVal) {
		value.set(newVal);
	}

	public void set(Object newVal) {
		set(objectToBigInteger(newVal));
	}

	public void set(byte[] newVal) {
		set(new BigInteger(newVal));
	}

	public void lazySet(BigInteger newVal) {
		set(newVal);
	}

	public void lazySet(Object newVal) {
		set(newVal);
	}

	public void lazySet(byte[] newVal) {
		set(newVal);
	}

	public boolean compareAndSet(BigInteger expect, BigInteger update) {
		while (true) {
			BigInteger origVal = get();

			if (origVal.compareTo(expect) == 0) {
				if (value.compareAndSet(origVal, update))
					return true;
			} else {
				return false;
			}
		}
	}

	public boolean compareAndSet(byte[] expect, byte[] update) {
		return compareAndSet(new BigInteger(expect), new BigInteger(update));
	}

	public boolean compareAndSet(Object expect, Object update) {
		return compareAndSet(objectToBigInteger(expect), objectToBigInteger(update));
	}

	public boolean weakCompareAndSet(BigInteger expect, BigInteger update) {
		return compareAndSet(expect, update);
	}

	public boolean weakCompareAndSet(byte[] expect, byte[] update) {
		return compareAndSet(expect, update);
	}

	public boolean weakCompareAndSet(Object expect, Object update) {
		return compareAndSet(expect, update);
	}

	public BigInteger getAndSet(BigInteger setVal) {
		while (true) {
			BigInteger origVal = get();

			if (compareAndSet(origVal, setVal))
				return origVal;
		}
	}

	public byte[] getAndSet(byte[] setVal) {
		return getAndSet(new BigInteger(setVal)).toByteArray();
	}

	public BigInteger getAndSet(Object setVal) {
		return getAndSet(objectToBigInteger(setVal));
	}

	public BigInteger getAndAdd(BigInteger delta) {
		while (true) {
			BigInteger origVal = get();
			BigInteger newVal = origVal.add(delta);
			if (compareAndSet(origVal, newVal))
				return origVal;
		}
	}

	public BigInteger addAndGet(BigInteger delta) {
		while (true) {
			BigInteger origVal = get();
			BigInteger newVal = origVal.add(delta);
			if (compareAndSet(origVal, newVal))
				return newVal;
		}
	}

	public byte[] getAndAdd(byte[] delta) {
		return getAndAdd(new BigInteger(delta)).toByteArray();
	}

	public byte[] addAndGet(byte[] delta) {
		return addAndGet(new BigInteger(delta)).toByteArray();
	}

	public BigInteger getAndAdd(Object delta) {
		return getAndAdd(objectToBigInteger(delta));
	}

	public BigInteger addAndGet(Object delta) {
		return addAndGet(objectToBigInteger(delta));
	}

	public BigInteger getAndIncrement() {
		return getAndAdd(BigInteger.ONE);
	}

	public BigInteger getAndDecrement() {
		return getAndAdd(BigInteger.ONE.negate());
	}

	public BigInteger incrementAndGet() {
		return addAndGet(BigInteger.ONE);
	}

	public BigInteger decrementAndGet() {
		return addAndGet(BigInteger.ONE.negate());
	}

	public double altGetAndIncrement() {
		return getAndIncrement().doubleValue();
	}

	public double altGetAndDecrement() {
		return getAndDecrement().doubleValue();
	}

	public double altIncrementAndGet() {
		return incrementAndGet().doubleValue();
	}

	public double altDecrementAndGet() {
		return decrementAndGet().doubleValue();
	}

	public BigInteger getAndMultiply(BigInteger multiplicand) {
		while (true) {
			BigInteger origVal = get();
			BigInteger newVal = origVal.multiply(multiplicand);
			if (compareAndSet(origVal, newVal))
				return origVal;
		}
	}

	public BigInteger multiplyAndGet(BigInteger multiplicand) {
		while (true) {
			BigInteger origVal = get();
			BigInteger newVal = origVal.multiply(multiplicand);
			if (compareAndSet(origVal, newVal))
				return newVal;
		}
	}

	public BigInteger getAndDivide(BigInteger divisor) {
		while (true) {
			BigInteger origVal = get();
			BigInteger newVal = origVal.divide(divisor);
			if (compareAndSet(origVal, newVal))
				return origVal;
		}
	}

	public BigInteger divideAndGet(BigInteger divisor) {
		while (true) {
			BigInteger origVal = get();
			BigInteger newVal = origVal.divide(divisor);
			if (compareAndSet(origVal, newVal))
				return newVal;
		}
	}

	public BigInteger getAndMultiply(Object multiplicand) {
		return getAndMultiply(objectToBigInteger(multiplicand));
	}

	public BigInteger multiplyAndGet(Object multiplicand) {
		return multiplyAndGet(objectToBigInteger(multiplicand));
	}

	public BigInteger getAndDivide(Object divisor) {
		return getAndDivide(objectToBigInteger(divisor));
	}

	public BigInteger divideAndGet(Object divisor) {
		return divideAndGet(objectToBigInteger(divisor));
	}

	// Methods of the Number class
	@Override
	public int intValue() {
		return getBigIntegerValue().intValue();
	}

	@Override
	public long longValue() {
		return getBigIntegerValue().longValue();
	}

	@Override
	public float floatValue() {
		return getBigIntegerValue().floatValue();
	}

	@Override
	public double doubleValue() {
		return getBigIntegerValue().doubleValue();
	}

	@Override
	public byte byteValue() {
		return (byte) intValue();
	}

	@Override
	public short shortValue() {
		return (short) intValue();
	}

	public char charValue() {
		return (char) intValue();
	}

	public BigInteger getBigIntegerValue() {
		return get();
	}

	public Double getDoubleValue() {
		return Double.valueOf(doubleValue());
	}

	public boolean isNaN() {
		return getDoubleValue().isNaN();
	}

	public boolean isInfinite() {
		return getDoubleValue().isInfinite();
	}

	// Methods of the BigInteger Class
	public BigInteger abs() {
		while (true) {
			BigInteger origVal = get();
			BigInteger newVal = origVal.abs();
			if (compareAndSet(origVal, newVal))
				return newVal;
		}
	}

	public BigInteger max(BigInteger val) {
		while (true) {
			BigInteger origVal = get();
			BigInteger newVal = origVal.max(val);
			if (compareAndSet(origVal, newVal))
				return newVal;
		}
	}

	public BigInteger min(BigInteger val) {
		while (true) {
			BigInteger origVal = get();
			BigInteger newVal = origVal.min(val);
			if (compareAndSet(origVal, newVal))
				return newVal;
		}
	}

	public BigInteger negate() {
		while (true) {
			BigInteger origVal = get();
			BigInteger newVal = origVal.negate();
			if (compareAndSet(origVal, newVal))
				return newVal;
		}
	}

	public BigInteger pow(int n) {
		while (true) {
			BigInteger origVal = get();
			BigInteger newVal = origVal.pow(n);
			if (compareAndSet(origVal, newVal))
				return newVal;
		}
	}

	// Support methods for hashing and comparing
	@Override
	public String toString() {
		return get().toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		try {
			return (compareTo(obj) == 0);
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return get().hashCode();
	}

	public int compareTo(AtomicBigInteger aValue) {
		return get().compareTo(aValue.get());
	}

	public int compareTo(Object aValue) {
		return get().compareTo(objectToBigInteger(aValue));
	}

	public int compareTo(byte[] aValue) {
		return get().compareTo(new BigInteger(aValue));
	}

	public static AtomicBigIntegerComparator comparator = new AtomicBigIntegerComparator();

	public static class AtomicBigIntegerComparator implements Comparator<Object> {
		public int compare(AtomicBigInteger d1, AtomicBigInteger d2) {
			return d1.compareTo(d2);
		}

		public int compare(Object d1, Object d2) {
			return objectToBigInteger(d1).compareTo(objectToBigInteger(d2));
		}

		public int compareReverse(AtomicBigInteger d1, AtomicBigInteger d2) {
			return d2.compareTo(d1);
		}

		public int compareReverse(Object d1, Object d2) {
			return objectToBigInteger(d2).compareTo(objectToBigInteger(d1));
		}
	}

	// Support Routines and constants
	private static BigInteger objectToBigInteger(Object obj) {
		if (obj instanceof AtomicBigInteger) {
			return ((AtomicBigInteger) obj).get();
		}
		if (obj instanceof BigInteger) {
			return (BigInteger) obj;
		} else if (obj instanceof BigInteger) {
			return new BigInteger(((BigInteger) obj).toByteArray());
		} else if (obj instanceof String) {
			return new BigInteger((String) obj);
		} else {
			return new BigInteger(obj.toString());
		}
	}
}
