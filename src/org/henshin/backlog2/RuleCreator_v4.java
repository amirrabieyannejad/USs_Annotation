package org.henshin.backlog2;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.eclipse.emf.henshin.model.compact.CModule;
import org.eclipse.emf.henshin.model.compact.CNode;
import org.eclipse.emf.henshin.model.compact.CRule;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/*  Delete Annotation for Attributes actions and entities
 *  plus their edges which have target edges or triggers edge
 *  This version is include US "Text"
 */
public class RuleCreator_v4 {
	private String jsonFile;
	private String henshinFile;
	private String eCoreFile;

	public RuleCreator_v4(String JsonFileName, String henshinFileName, String eCoreFileName) {
		jsonFile = JsonFileName;
		henshinFile = henshinFileName;
		eCoreFile = eCoreFileName;
	}

	public String getJsonFile() {
		return jsonFile;
	}

	public String getJsonFileAbsolutePath() throws EmptyOrNotExistJsonFile {
		Path path = Paths.get("C:\\Users\\amirr\\eclipse-workspace_new\\" + "org.henshin.backlog2\\" + getJsonFile());
		if (Files.exists(path)) {
			return path.toString();

		} else {
			throw new EmptyOrNotExistJsonFile();
		}

	}

	public String getEcoreFileAbsolutePath() {
		Path path = Paths.get("C:\\Users\\amirr\\eclipse-workspace_new\\" + "org.henshin.backlog2\\" + getEcoreFile());
		if (Files.exists(path)) {
			return path.toString();

		}
		return null;

	}

	public String getHenshinFile() {
		return henshinFile;
	}

	public String getEcoreFile() {
		return eCoreFile;
	}

	// private static final Logger LOGGER =
	// Logger.getLogger(RuleCreator_v4.class.getName());

	public static void main(String[] args) throws IOException, EcoreFileNotFound, EmptyOrNotExistJsonFile,
			PersonaInJsonFileNotFound, UsNrInJsonFileNotFound, ActionInJsonFileNotFound, EntityInJsonFileNotFound,
			TargetsInJsonFileNotFound, ContainsInJsonFileNotFound, TextInJsonFileNotFound, TriggersInJsonFileNotFound,
			EdgeWithSameSourceAndTarget {
		String version= "28";
		RuleCreator_v4 ruleCreator = new 
				RuleCreator_v4("Datasets\\g"+version+"_baseline_pos.json",
						"Henshin_backlog_g"+version,
				"Backlog_v2.3.ecore");
		JSONArray jsonArray = ruleCreator.readJsonArrayFromFile();
		CModule cModule = ruleCreator.processJsonFile(jsonArray);
		cModule.save();

	}

	public JSONArray readJsonArrayFromFile() throws EmptyOrNotExistJsonFile {
		JSONArray jsonArray;
		try (FileReader reader = new FileReader(getJsonFileAbsolutePath())) {
			JSONTokener tokener = new JSONTokener(reader);
			if (!tokener.more()) {
				throw new EmptyOrNotExistJsonFile();

			}
			// Read JSON file
			jsonArray = new JSONArray(tokener);

			return jsonArray;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public CModule assignCmodule() throws EcoreFileNotFound {
		CModule module = new CModule(getHenshinFile());
		if (getEcoreFileAbsolutePath() == null) {
			throw new EcoreFileNotFound();
		}
		module.addImportsFromFile(getEcoreFile());
		return module;

	}

	public CModule processJsonFile(JSONArray json)
			throws EcoreFileNotFound, PersonaInJsonFileNotFound, UsNrInJsonFileNotFound, ActionInJsonFileNotFound,
			EntityInJsonFileNotFound, TargetsInJsonFileNotFound, ContainsInJsonFileNotFound, TextInJsonFileNotFound,
			TriggersInJsonFileNotFound, EdgeWithSameSourceAndTarget {
		CModule cModule = assignCmodule();
		String usNrM = null;
		CRule userStoryM = null;
		JSONArray personaM = null;
		JSONObject actionM = null;
		JSONObject entityM = null;
		JSONArray targetsArrayM = null;
		JSONArray containsArrayM = null;
		String textM = null;
		for (int i = 0; i < json.length(); i++) {
			try {
				JSONObject jsonObject = json.getJSONObject(i);

				if (jsonObject.has("US_Nr")) {

					usNrM = jsonObject.getString("US_Nr");
					userStoryM = processRule(jsonObject, usNrM, cModule);
				} else {
					throw new UsNrInJsonFileNotFound();
				}

				if (jsonObject.has("Persona")) {
					personaM = jsonObject.getJSONArray("Persona");
				} else {
					throw new PersonaInJsonFileNotFound();
				}

				if (jsonObject.has("Action")) {
					actionM = jsonObject.getJSONObject("Action");
				} else {
					throw new ActionInJsonFileNotFound();
				}

				if (jsonObject.has("Entity")) {
					entityM = jsonObject.getJSONObject("Entity");

				} else {
					throw new EntityInJsonFileNotFound();
				}
				if (jsonObject.has("Triggers")) {
					targetsArrayM = jsonObject.getJSONArray("Triggers");
				} else {
					throw new TriggersInJsonFileNotFound();
				}
				if (jsonObject.has("Targets")) {
					targetsArrayM = jsonObject.getJSONArray("Targets");
				} else {
					throw new TargetsInJsonFileNotFound();
				}
				if (jsonObject.has("Contains")) {
					containsArrayM = jsonObject.getJSONArray("Contains");
				} else {
					throw new ContainsInJsonFileNotFound();
				}
				if (jsonObject.has("Text")) {
					textM = jsonObject.getString("Text");
				} else {
					throw new TextInJsonFileNotFound();
				}

				CNode personaNode = processPersona(jsonObject, personaM, userStoryM, usNrM);
				// store all entities in one map which the string is the name of entities and
				// CNode correspond to their CNode Object
				Map<String, CNode> entityMap = new HashMap<>();
				// store all actions in one map which the string is the name of actions and
				// CNode correspond to their CNode Object
				Map<String, CNode> actionMap = new HashMap<>();
				processText(jsonObject, userStoryM, textM);
				processActions(jsonObject, userStoryM, actionM, personaNode, actionMap, usNrM);
				processEntities(jsonObject, userStoryM, entityM, targetsArrayM, entityMap, usNrM);
				processTargetsEdges(jsonObject, targetsArrayM, entityMap, actionMap, usNrM);
				processContainsEdges(jsonObject, containsArrayM, targetsArrayM, entityMap, usNrM);
			} catch (JSONException e) {
				e.printStackTrace();
			}

		}
		return cModule;
	}

	// Make rules which as name have Us_Nr Object in JSON file
	private CRule processRule(JSONObject jsonObject, String usNr, CModule module) {
		try {

			CRule userStory = module.createRule(usNr);
			return userStory;

		} catch (NullPointerException e) {
			e.printStackTrace();
			return null;
		}
	}

	// Create node as a story with an attribute called "text" that contains the
	// "Text"
	// JSONObject of the JSON file
	private void processText(JSONObject jsonObject, CRule userStory, String text) {
		try {
			CNode nodeText = userStory.createNode("Story");
			text = text.replaceAll(" $", "").replaceAll("^ ", "").toLowerCase();
			nodeText.createAttribute("text", "\"" + text + "\"");
		} catch (JSONException e) {
			e.printStackTrace();
		}

	}

	private CNode processPersona(JSONObject jsonObject, JSONArray persona, CRule userStory, String usNr) {
		try {

			CNode nodePersona = userStory.createNode("Persona");
			String person = persona.getString(0).replaceAll(" $", "").replaceAll("^ ", "").toLowerCase();
			nodePersona.createAttribute("name", "\"" + person + "\"");
			return nodePersona;

		} catch (JSONException e) {
			e.printStackTrace();

		}
		return null;
	}

	private void processActions(JSONObject jsonObject, CRule userStory, JSONObject action, CNode nodePersona,
			Map<String, CNode> actionMap, String usNrM) throws ActionInJsonFileNotFound, EdgeWithSameSourceAndTarget {
		try {
			// CNode abstractAction = userStory.createNode("Action");
			if (action.has("Primary Action")) {
				JSONArray primaryAction = action.getJSONArray("Primary Action");
				// Creating Nodes for Primary Action/s
				for (int i = 0; i < primaryAction.length(); i++) {
					String priAction = primaryAction.getString(i).replaceAll(" $", "").replaceAll("^ ",
							"").toLowerCase();
					CNode cNode = userStory.createNode("Primary Action");

					cNode.createAttribute("name", "\"" + priAction + "\"", "delete");

					// Create Edges from Action to Primary Action names
					// primary actions

					try {
						nodePersona.createEdge(cNode, "triggers", "delete");
					} catch (RuntimeException e) {

						throw new EdgeWithSameSourceAndTarget(
								"Edge with Action: \"" + primaryAction.getString(i).toLowerCase()
										+ "\" and with Persona" + " is already created!");
					}
					actionMap.put(priAction, cNode);

				}
			} else {
				throw new ActionInJsonFileNotFound("Primary Action in JOSNObject not found!");
			}
			if (action.has("Secondary Action")) {
				// Creating Nodes for Secondary Action/s
				JSONArray secondaryAction = action.getJSONArray("Secondary Action");
				for (int i = 0; i < secondaryAction.length(); i++) {
					String secAction = secondaryAction.getString(i).replaceAll(" $", "").replaceAll("^ ",
							"").toLowerCase();
					CNode cNode = userStory.createNode("Secondary Action");
					cNode.createAttribute("name", "\"" + secAction + "\"", "delete");

					actionMap.put(secAction, cNode);
				}
			} else {
				System.out.println("Secondary Action not found!");
				throw new ActionInJsonFileNotFound("Secondary Action in JOSNObject not found!");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void processEntities(JSONObject jsonObject, CRule userStory, JSONObject entity, JSONArray targetsArray,
			Map<String, CNode> entityMap, String usNrM) throws EntityInJsonFileNotFound {
		try {

			if (entity.has("Primary Entity")) {
				JSONArray primaryEntity = entity.getJSONArray("Primary Entity");

				// Creating Nodes for Primary Entity/s
				for (int i = 0; i < primaryEntity.length(); i++) {
					CNode cNode = null;
					String priEntity = primaryEntity.getString(i).replaceAll(" $", "").replaceAll("^ ",
							"").toLowerCase();
					// check if entity exist in entityMap
					if (checkEntityIsTarget(primaryEntity.getString(i), targetsArray)) {
						cNode = userStory.createNode("Primary Entity");
						cNode.createAttribute("name", "\"" + priEntity + "\"", "delete");
						entityMap.put(priEntity, cNode);
					} else {
						cNode = userStory.createNode("Primary Entity");
						cNode.createAttribute("name", "\"" + priEntity + "\"");
						entityMap.put(priEntity, cNode);
					}
				}
			} else {
				throw new EntityInJsonFileNotFound("Primary Entity in JSON file not found!");
			}
			if (entity.has("Secondary Entity")) {
				// Creating Nodes for Secondary Entity/ies
				JSONArray secondaryEntity = entity.getJSONArray("Secondary Entity");
				// Creating Nodes for Primary Entity/s
				for (int i = 0; i < secondaryEntity.length(); i++) {
					String secEntity = secondaryEntity.getString(i).toLowerCase().replaceAll(" $", "").replaceAll("^ ",
							"");
					CNode cNode = null;
					// check if entity exist in enttiyMap
					if (checkEntityIsTarget(secEntity, targetsArray)) {

						cNode = userStory.createNode("Secondary Entity");
						cNode.createAttribute("name", "\"" + secEntity + "\"", "delete");
						entityMap.put(secEntity, cNode);
					} else {

						cNode = userStory.createNode("Secondary Entity");

						cNode.createAttribute("name", "\"" + secEntity + "\"");
						entityMap.put(secEntity, cNode);

					}

				}
			} else {

				throw new EntityInJsonFileNotFound("Secondary Entity in JSON file not found!");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

	}

	private void processTargetsEdges(JSONObject jsonObject, JSONArray targetsArray, Map<String, CNode> entityMap,
			Map<String, CNode> actionMap, String usNrM)
			throws EntityInJsonFileNotFound, EdgeWithSameSourceAndTarget, ActionInJsonFileNotFound {

		for (int i = 0; i < targetsArray.length(); i++) {
			JSONArray currentArray = targetsArray.getJSONArray(i);
			// replace space at the end of text if any
			String action = currentArray.getString(0).replaceAll(" $", "").replaceAll("^ ", "").toLowerCase();
			String entity = currentArray.getString(1).replaceAll(" $", "").replaceAll("^ ", "").toLowerCase();
			CNode nodeEntity = null;
			CNode nodeAction = null;
			// check if action and entity are exist in corresponding JSON Object in file
			if ((actionMap.get(action) != null)) {
				nodeAction = actionMap.get(action);
			} else {
				throw new ActionInJsonFileNotFound(
						"In \"Targets\" of " + usNrM + ", Action: \"" + action.toString() + "\" is not found!");
			}

			if ((entityMap.get(entity) != null)) {

				nodeEntity = entityMap.get(entity);
			} else {

				throw new EntityInJsonFileNotFound(
						"In \"Targets\" of " + usNrM + ", Entity: \"" + entity.toString() + "\" is not found!");
			}

			// throw exception if Failed to create Edge
			try {
				nodeAction.createEdge(nodeEntity, "targets", "delete");
			} catch (RuntimeException e) {

				throw new EdgeWithSameSourceAndTarget("In \"Targets\" of " + usNrM + ", Edge with Action: \""
						+ action.toString() + "\" and with Entity: \"" + entity.toString() + "\" is already created!");
			}

		}
	}

	private void processContainsEdges(JSONObject jsonObject, JSONArray containsArray, JSONArray targetsArray,
			Map<String, CNode> entityMap, String usNrM) throws EntityInJsonFileNotFound, EdgeWithSameSourceAndTarget {

		// iterate through contains JSONArray
		for (int i = 0; i < containsArray.length(); i++) {
			JSONArray currentArray = containsArray.getJSONArray(i);

			// consider the first element of array as firstEnttiy
			// replace space at the end of text if any
			String firstEntity = currentArray.getString(0).replaceAll(" $", "").replaceAll("^ ", "").toLowerCase();
			// consider the first element of array as secondEnttiy
			// replace space at the end of text if any
			String secondEntity = currentArray.getString(1).replaceAll(" $", "").replaceAll("^ ", "").toLowerCase();

			// make sure that both entity is already listed in entityMap
			if ((entityMap.get(firstEntity) != null) && (entityMap.get(secondEntity) != null)) {
				CNode nodefirstEntity = entityMap.get(firstEntity);
				CNode nodeSecondEntity = entityMap.get(secondEntity);

				// Check if any Entity in Contains is already exist in Targets
				// if so annotate the contains edge as <delete>
				// otherwise annotate the contains edge as <preserve>
				if (checkEntityIsTarget(firstEntity, targetsArray) || checkEntityIsTarget(secondEntity, targetsArray)) {

					try {
						// add an edge from first Entity to second Entity and annotated it as<delete>
						nodefirstEntity.createEdge(nodeSecondEntity, "contains", "delete");
					} catch (RuntimeException e) {

						throw new EdgeWithSameSourceAndTarget("In \"Contains\" of " + usNrM + ", Edge with Entity: \""
								+ firstEntity.toLowerCase().toString() + "\" and Entity \"" + secondEntity.toString()
								+ "\" is already created!");
					}

				} else {
					try {
						// add an edge from first Entity to second Entity and annotated it as<preserve>
						nodefirstEntity.createEdge(nodeSecondEntity, "contains");
					} catch (RuntimeException e) {

						throw new EdgeWithSameSourceAndTarget("In \"Contains\" of " + usNrM + ", Edge with Entity: \""
								+ firstEntity.toLowerCase().toString() + "\" and Entity" + secondEntity.toString()
								+ " is already created!");
					}

				}
			} else {
				throw new EntityInJsonFileNotFound("In " + usNrM + ", following entties are missing: "
						+ firstEntity.toString() + " and " + secondEntity.toString());
			}
		}

	}

// check if Entity is 
	private static boolean checkEntityIsTarget(String entity, JSONArray targets) {
		for (int j = 0; j < targets.length(); j++) {
			// iterate through each element in array
			JSONArray currentArray = targets.getJSONArray(j);
			String targetEntity = currentArray.getString(1);
			if (targetEntity.equals(entity)) {
				return true;
			}
		}
		return false;
	}

}
