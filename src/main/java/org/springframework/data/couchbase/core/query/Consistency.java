/*
 * Copyright 2012-2020 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.couchbase.core.query;

import com.couchbase.client.java.query.QueryScanConsistency;
import com.couchbase.client.java.view.ViewScanConsistency;

/**
 * Enumeration of different consistency configurations to be used by the queries generated by the framework. Each
 * consistency can be translated to a {@link ViewScanConsistency} (for the {@link #viewConsistency() views}) and
 * {@link QueryScanConsistency} (for the {@link #n1qlConsistency() N1QL queries}).
 *
 * @author Simon Baslé
 */
public enum Consistency {
	// TODO: Needs to be reviewed carefully..

	/** READ_YOUR_OWN_WRITES is {@link ViewScanConsistency#REQUEST_PLUS} and {@link QueryScanConsistency#REQUEST_PLUS} */
	READ_YOUR_OWN_WRITES(ViewScanConsistency.REQUEST_PLUS, QueryScanConsistency.REQUEST_PLUS),
	/** STRONGLY_CONSISTENT is {@link ViewScanConsistency#REQUEST_PLUS} and {@link QueryScanConsistency#REQUEST_PLUS} */
	STRONGLY_CONSISTENT(ViewScanConsistency.REQUEST_PLUS, QueryScanConsistency.REQUEST_PLUS),
	/** UPDATE_AFTER is {@link ViewScanConsistency#UPDATE_AFTER} and {@link QueryScanConsistency#NOT_BOUNDED} */
	UPDATE_AFTER(ViewScanConsistency.UPDATE_AFTER, QueryScanConsistency.NOT_BOUNDED),
	/** EVENTUALLY_CONSISTENT is {@link ViewScanConsistency#UPDATE_AFTER} and {@link QueryScanConsistency#NOT_BOUNDED} */
	EVENTUALLY_CONSISTENT(ViewScanConsistency.UPDATE_AFTER, QueryScanConsistency.NOT_BOUNDED);

	/**
	 * The static default Consistency ({@link #READ_YOUR_OWN_WRITES}).
	 */
	public static final Consistency DEFAULT_CONSISTENCY = READ_YOUR_OWN_WRITES;

	private final ViewScanConsistency viewConsistency;
	private final QueryScanConsistency n1qlConsistency;

	Consistency(ViewScanConsistency viewConsistency, QueryScanConsistency n1qlConsistency) {
		this.viewConsistency = viewConsistency;
		this.n1qlConsistency = n1qlConsistency;
	}

	/**
	 * Returns the {@link ViewScanConsistency view consistency} corresponding to this {@link Consistency}.
	 *
	 * @return the view consistency.
	 */
	public ViewScanConsistency viewConsistency() {
		return viewConsistency;
	}

	/**
	 * Returns the {@link QueryScanConsistency consistency} corresponding to this {@link Consistency}.
	 *
	 * @return the N1QL consistency.
	 */
	public QueryScanConsistency n1qlConsistency() {
		return n1qlConsistency;
	}
}
