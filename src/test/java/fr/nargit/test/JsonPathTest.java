package fr.nargit.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flipkart.zjsonpatch.JsonPatch;

/**
 * (c) Swissquote 01-sept.-2015
 *
 * @author ttchougourian
 */
public class JsonPathTest {

	private Date birthdate = new Date(1987, Calendar.JULY, 3);

	@Test
	public void partialUpdate() throws IOException {
		JsonNode patches = convertToJson(clientUpdates());

		validatePatches(patches);

		PersonEntity oldPerson = getPersonFromDatabase();
		JsonNode person = convertToJson(oldPerson);

		// apply patch
		JsonNode patchedPerson = JsonPatch.apply(patches, person);
		PersonEntity newPerson = convertToEntity(patchedPerson);

		Assert.assertThat(newPerson.firstname, CoreMatchers.is("tyler"));
		Assert.assertThat(newPerson.lastname, CoreMatchers.is(""));
		Assert.assertThat(newPerson.young, CoreMatchers.is(false));
		Assert.assertThat(newPerson.iam, CoreMatchers.nullValue());
		Assert.assertThat(newPerson.birthDate, CoreMatchers.is(this.birthdate));

		doBusinessStuff(oldPerson, newPerson);

	}

	@Test
	public void partialRead() throws IOException {
		List<String> requestedFields = clientRequest();

		validateFields(requestedFields);

		PersonEntity oldPerson = getPersonFromDatabase();
		final JsonNode person = convertToJson(oldPerson);

		JsonNode partialPerson = new ObjectMapper().valueToTree(new PersonEntity());
		// get only usefull fields
		for (String field : requestedFields) {
			((ObjectNode) partialPerson).put(field, person.get(field).textValue());
		}
/* java 8 style
		requestedFields.forEach((field) -> {
			((ObjectNode) partialPerson).put(field, person.get(field).textValue());
		});
*/
		PersonEntity newPerson = convertToEntity(partialPerson);

		Assert.assertThat(newPerson.firstname, CoreMatchers.is("tigran"));
		Assert.assertThat(newPerson.lastname, CoreMatchers.is("durden"));
		Assert.assertThat(newPerson.young, CoreMatchers.nullValue());
		Assert.assertThat(newPerson.iam, CoreMatchers.nullValue());
		Assert.assertThat(newPerson.birthDate, CoreMatchers.nullValue());

		doBusinessStuff(oldPerson, newPerson);

	}

	private void validatePatches(JsonNode patches) {
		if (patches.has("birthDate")) {
			throw new IllegalArgumentException("You cannot update this field");
		}
	}

	private void validateFields(List<String> fields) {
		if (fields.contains("birthDate")) {
			throw new IllegalArgumentException("You cannot update this field");
		}
	}

	private void doBusinessStuff(PersonEntity oldPerson, PersonEntity newPerson) {
		// business specific need
		if (oldPerson.firstname.equals(newPerson.firstname)) {
			sendIssueWithDiff(oldPerson.firstname, newPerson.firstname);
		}
	}

	private void sendIssueWithDiff(String oldFirstName, String newFirstName) {
		// do some business Stuff
	}

	private PersonEntity convertToEntity(JsonNode patchedPerson) throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		// json to entity
		return objectMapper.treeToValue(patchedPerson, PersonEntity.class);
	}

	private JsonNode convertToJson(PersonEntity personEntity) {
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.valueToTree(personEntity);
	}

	private PersonEntity getPersonFromDatabase() {
		// Our database
		PersonEntity personEntity = new PersonEntity();
		personEntity.firstname = "tigran";
		personEntity.lastname = "durden";
		personEntity.young = true;
		personEntity.iam = 1L;
		personEntity.birthDate = this.birthdate;
		return personEntity;
	}

	private JsonNode convertToJson(String patches) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.readTree(patches);
	}

	private String clientUpdates() {
		// patches to apply object
		return "[" +
				"{ \"op\": \"replace\", \"path\": \"firstname\", \"value\": \"tyler\" }," +
				"{ \"op\": \"replace\", \"path\": \"lastname\", \"value\": \"\" }, " +
				"{ \"op\": \"replace\", \"path\": \"young\", \"value\": false }, " +
				"{ \"op\": \"replace\", \"path\": \"iam\", \"value\": null } " +
				"]";
	}

	private List<String> clientRequest() {
		// patches to apply object
		return new ArrayList<String>() {{
			add("firstname");
			add("lastname");
		}};
	}

	static class PersonEntity {
		public String firstname;
		public String lastname;
		public Boolean young;
		public Long iam;
		public Date birthDate;
	}
}
