/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package example.springdata.cassandra.basic;

import example.springdata.cassandra.util.RequiresCassandraKeyspace;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Version;
import org.springframework.test.context.junit4.SpringRunner;

import com.datastax.driver.core.Session;

import example.springdata.cassandra.util.CassandraVersion;

/**
 * Integration test showing the basic usage of {@link BasicUserRepository}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Christoph Strobl
 * @author Mark Paluch
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = BasicConfiguration.class)
public class BasicUserRepositoryTests {

	public final static Version CASSANDRA_3_4 = Version.parse("3.4");

	@ClassRule
	public final static RequiresCassandraKeyspace CASSANDRA_KEYSPACE = RequiresCassandraKeyspace.onLocalhost();

	@Autowired BasicUserRepository repository;
	@Autowired Session session;
	User user;

	@Before
	public void setUp() {

		user = new User();
		user.setId(42L);
		user.setUsername("foobar");
		user.setFirstname("firstname");
		user.setLastname("lastname");
	}

	/**
	 * Saving an object using the Cassandra Repository will create a persistent representation of the object in Cassandra.
	 */
	@Test
	public void findSavedUserById() {

		user = repository.save(user);

		assertThat(repository.findOne(user.getId()), is(user));
	}

	/**
	 * Cassandra can be queries by using query methods annotated with {@link @Query}.
	 */
	@Test
	public void findByAnnotatedQueryMethod() {

		repository.save(user);

		assertThat(repository.findUserByIdIn(1000), is(nullValue()));
		assertThat(repository.findUserByIdIn(42), is(equalTo(user)));
	}

	/**
	 * Spring Data Cassandra supports query derivation so annotating query methods with
	 * {@link org.springframework.data.cassandra.repository.Query} is optional. Querying columns other than the primary
	 * key requires a secondary index.
	 */
	@Test
	public void findByDerivedQueryMethod() {

		session.execute("CREATE INDEX IF NOT EXISTS user_username ON users (uname);");
		repository.save(user);

		assertThat(repository.findUserByUsername(user.getUsername()), is(user));
	}

	/**
	 * Spring Data Cassandra supports {@code LIKE} and {@code CONTAINS} query keywords to for SASI indexes.
	 */
	@Test
	public void findByDerivedQueryMethodWithSASI() {

		assumeTrue(CassandraVersion.getReleaseVersion(session).isGreaterThanOrEqualTo(CASSANDRA_3_4));

		session.execute("CREATE CUSTOM INDEX ON users (lname) USING 'org.apache.cassandra.index.sasi.SASIIndex';");
		repository.save(user);

		assertThat(repository.findUsersByLastnameStartsWith("last"), hasItem(user));
	}
}
