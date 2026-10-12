package top.tasaed.aoh2de.llm.playing.mcp;

import java.util.List;
import java.util.Map;

public final class HandlerMcpTools {
    public static final McpToolDefinition TURN_STATS = new McpToolDefinition(
            "turn_get_stats",
            "Query the current turn phase and end-turn button state.",
            object(Map.of(), List.of()),
            ToolKind.READ_ONLY);

    public static final McpToolDefinition END_TURN = new McpToolDefinition(
            "turn_click_end_turn",
            "Click the game's End Turn button, including when handling game events. Check turn_get_stats afterward: "
                    + "resolution can take a long time and may produce events to review.",
            object(Map.of(), List.of()),
            ToolKind.WRITE);

    public static final McpToolDefinition ARMY_LIST = new McpToolDefinition(
            "army_get_army_list",
            "List the player's non-zero armies, including armies stationed in foreign provinces.",
            object(Map.of(), List.of()),
            ToolKind.READ_ONLY);

    public static final McpToolDefinition MOVE_ARMY = new McpToolDefinition(
            "army_move",
            "Order army movement during INPUT_ORDERS. The game validates the route, controlled troops and movement points.",
            object(
                    Map.of(
                            "fromProvinceId",
                                    provinceId("Source province; must contain an army controlled by the player."),
                            "toProvinceId", provinceId("Destination province; must differ from the source."),
                            "units",
                                    Map.of(
                                            "type", "integer",
                                            "minimum", 1,
                                            "description",
                                                    "Troop count; must not exceed the troops available for this order."),
                            "moveTo",
                                    Map.of(
                                            "type",
                                            "boolean",
                                            "default",
                                            false,
                                            "description",
                                            "Allow a multi-hop route and create an automatic marching route. "
                                                    + "When false, the destination must be adjacent.")),
                    List.of("fromProvinceId", "toProvinceId", "units")),
            ToolKind.WRITE);

    public static final McpToolDefinition CANCEL_ARMY_MOVE = new McpToolDefinition(
            "army_cancel_move",
            "Cancel an ordered army movement during INPUT_ORDERS.",
            object(
                    Map.of(
                            "fromProvinceId", provinceId("Source province of the ordered movement."),
                            "toProvinceId",
                                    provinceId(
                                            "Final destination of the ordered movement, including multi-hop routes.")),
                    List.of("fromProvinceId", "toProvinceId")),
            ToolKind.WRITE);

    public static final McpToolDefinition CONSTRUCT_BUILDING = new McpToolDefinition(
            "building_construct",
            "Construct or upgrade a building by one level in a player-owned, unoccupied land province during INPUT_ORDERS. "
                    + "Terrain, technology, building level, resources and construction availability are validated by the game.",
            object(
                    Map.of(
                            "provinceId", provinceId("Player-owned, unoccupied land province in which to build."),
                            "buildingType",
                                    enumeration(
                                            List.of(
                                                    "farm",
                                                    "port",
                                                    "fort",
                                                    "tower",
                                                    "library",
                                                    "armoury",
                                                    "workshop",
                                                    "market",
                                                    "supply"),
                                            "Building to construct or upgrade. Input is trimmed and converted to lowercase by the service.")),
                    List.of("provinceId", "buildingType")),
            ToolKind.WRITE);

    public static final McpToolDefinition DIPLOMACY_STATS = new McpToolDefinition(
            "diplomacy_get_stats",
            "Get the player's diplomatic status with the specified civilization.",
            object(Map.of("civilizationId", civilizationId()), List.of("civilizationId")),
            ToolKind.READ_ONLY);

    public static final McpToolDefinition DECLARE_WAR = new McpToolDefinition(
            "diplomacy_declare_war",
            "Declare war on the target civilization. Confirm it is not already at war and has no truce first.",
            object(Map.of("civilizationId", civilizationId()), List.of("civilizationId")),
            ToolKind.WRITE);

    public static final McpToolDefinition CHANGE_RELATION = new McpToolDefinition(
            "diplomacy_change_relation",
            "Improve relations or send a diplomatic insult. An insult cancels both civilizations' improve-relations actions "
                    + "and suspends diplomatic relations. Improving relations fails with DIPLOMATIC_RELATIONS_SUSPENDED "
                    + "when the target has closed its embassy to the player. Diplomacy-point costs follow game configuration.",
            object(
                    Map.of(
                            "civilizationId", civilizationId(),
                            "type",
                                    enumeration(
                                            List.of("improve", "decrease"),
                                            "improve starts an improve-relations action; decrease sends a diplomatic insult."),
                            "turns",
                                    integer(
                                            "Valid range depends on action and game configuration: improve requires 1 through "
                                                    + "gvRelationImprove.IMPROVE_RELATIONS_MAX_NUM_OF_TURNS; decrease requires "
                                                    + "gvRelationDecrease.SUSPEND_DIPLOMATIC_RELATIONS_MIN through "
                                                    + "gvRelationDecrease.SUSPEND_DIPLOMATIC_RELATIONS_MAX.")),
                    List.of("civilizationId", "type", "turns")),
            ToolKind.WRITE);

    public static final McpToolDefinition TREATY_ACTION = new McpToolDefinition(
            "diplomacy_action_treaty",
            "Send a treaty proposal from the current player to another living civilization. Success means sent, not active: "
                    + "the recipient must accept through the game's message workflow. Costs and duration limits follow game "
                    + "configuration; guarantee independence uses the actual sender's military-access ask cost. Existing "
                    + "treaties do not prevent proposals; acceptance overwrites duration rather than choosing the longer duration. "
                    + "Military access keeps per-turn maintenance costs and separate records for opposite directions.",
            object(
                    Map.of(
                            "civilizationId",
                                    integer(
                                            "Internal game civilization ID, not its display name; must be another living civilization."),
                            "type",
                                    enumeration(
                                            List.of(
                                                    "ask_military_access",
                                                    "offer_military_access",
                                                    "defensive_pact",
                                                    "non_aggression_pact",
                                                    "guarantee_independence"),
                                            "ask_military_access asks permission for the player to enter the recipient; "
                                                    + "offer_military_access offers the recipient permission to enter the player. "
                                                    + "defensive_pact and non_aggression_pact are symmetric on acceptance; only the "
                                                    + "defensive pact joins defensive wars. guarantee_independence proposes that the "
                                                    + "player guarantees the recipient, not the reverse, and joins defensive wars."),
                            "turns",
                                    Map.of(
                                            "type",
                                            "integer",
                                            "minimum",
                                            1,
                                            "description",
                                            "Maximum follows gvDipMilitaryAccess for both access operations, "
                                                    + "gvDipDefensivePact for defensive pact, gvDipNonAggression for non-aggression "
                                                    + "pact, and gvDipGuarantee for guarantee independence.")),
                    List.of("civilizationId", "type", "turns")),
            ToolKind.WRITE);

    public static final McpToolDefinition SELF_SUMMARY = new McpToolDefinition(
            "self_get_summary",
            "Get the current player's civilization summary, including gold, movement points and diplomacy points.",
            object(Map.of(), List.of()),
            ToolKind.READ_ONLY);

    public static final McpToolDefinition BUDGET_SPENDING = new McpToolDefinition(
            "self_set_budget_spending",
            "Set Taxes, Goods, Research and Investments sliders during INPUT_ORDERS. Military spending is preserved; "
                    + "Goods, Research and Investments plus military spending must fit within the game's budget limit "
                    + "(normally 100%). When the budget is non-positive, those three spending values are set to zero, matching the game UI.",
            object(
                    Map.of(
                            "goods", percentage("Goods spending percentage."),
                            "research", percentage("Research spending percentage."),
                            "investments", percentage("Investments spending percentage."),
                            "taxes", percentage("Taxation level percentage.")),
                    List.of("goods", "research", "investments", "taxes")),
            ToolKind.WRITE);

    public static final McpToolDefinition BUDGET_SPENDING_INFO = new McpToolDefinition(
            "self_get_budget_spending_info",
            "Get current budget spending and negative-growth thresholds. Goods below min_goods reduce population growth; "
                    + "Investments below min_investments reduce economy growth; Taxes above max_taxes reduce happiness growth. "
                    + "Thresholds depend on ideology, religion and administration policy.",
            object(Map.of(), List.of()),
            ToolKind.READ_ONLY);

    public static final McpToolDefinition CIVILIZATION_VIEW = new McpToolDefinition(
            "self_get_civilization_view",
            "Get the player's comprehensive CivilizationView statistics: territory, population, military, economy, "
                    + "technology, development, inflation, nuclear weapons, happiness, stability, ranking, sanctions, "
                    + "government, religion and game settings.",
            object(Map.of(), List.of()),
            ToolKind.READ_ONLY);

    public static final McpToolDefinition MESSAGE_LIST = new McpToolDefinition(
            "message_get_message_list",
            "List messages waiting in the player's diplomatic message box. Use messageId with message_action_message.",
            object(Map.of(), List.of()),
            ToolKind.READ_ONLY);

    public static final McpToolDefinition MESSAGE_ACTION = new McpToolDefinition(
            "message_action_message",
            "Accept or decline a message and remove it from the player's message box. IDs are current list indexes "
                    + "and may change whenever a message is removed; query message_get_message_list again before later actions.",
            object(
                    Map.of(
                            "messageId",
                                    Map.of(
                                            "type", "integer",
                                            "minimum", 0,
                                            "description",
                                                    "Current zero-based index returned by message_get_message_list."),
                            "type",
                                    enumeration(
                                            List.of("accept", "decline"), "Whether to accept or decline the message.")),
                    List.of("messageId", "type")),
            ToolKind.WRITE);

    public static final McpToolDefinition NATION_INFORMATION = new McpToolDefinition(
            "nation_get_nation_information",
            "Get a civilization's basic information; omit civilizationId to use the current player's civilization.",
            optionalCivilization(),
            ToolKind.READ_ONLY);

    public static final McpToolDefinition PROVINCE_LIST = new McpToolDefinition(
            "nation_get_province_list",
            "List provinces owned by a civilization; omit civilizationId to use the current player's civilization.",
            optionalCivilization(),
            ToolKind.READ_ONLY);

    public static final McpToolDefinition NEIGHBOR_CIVS = new McpToolDefinition(
            "nation_get_neighbor_civs",
            "List neighboring civilizations of a civilization; omit civilizationId to use the current player's civilization.",
            optionalCivilization(),
            ToolKind.READ_ONLY);

    public static final McpToolDefinition PROVINCE_INFORMATION = new McpToolDefinition(
            "province_get_information",
            "Get information about the specified province.",
            object(Map.of("provinceId", provinceId("Province whose information is requested.")), List.of("provinceId")),
            ToolKind.READ_ONLY);

    public static final McpToolDefinition CURRENT_EVENT = new McpToolDefinition(
            "event_get_current_event",
            "Get armies participating in the current military event, their combined attacking troops and the defending "
                    + "civilization. Returns NO_EVENT when no event is active.",
            object(Map.of(), List.of()),
            ToolKind.READ_ONLY);

    private HandlerMcpTools() {}

    private static Map<String, Object> object(Map<String, Object> properties, List<String> required) {
        return Map.of("type", "object", "properties", properties, "required", required, "additionalProperties", false);
    }

    private static Map<String, Object> integer(String description) {
        return Map.of("type", "integer", "description", description);
    }

    private static Map<String, Object> civilizationId() {
        return integer("Internal game civilization ID, not its display name.");
    }

    private static Map<String, Object> provinceId(String description) {
        return integer(
                "Internal game province ID, not its display name; must be within the current game's province range. "
                        + description);
    }

    private static Map<String, Object> optionalCivilization() {
        return object(
                Map.of(
                        "civilizationId",
                        integer("Internal game civilization ID, not its display name. "
                                + "When omitted, the current player's civilization is used.")),
                List.of());
    }

    private static Map<String, Object> enumeration(List<String> values, String description) {
        return Map.of("type", "string", "enum", values, "description", description);
    }

    private static Map<String, Object> percentage(String description) {
        return Map.of("type", "number", "minimum", 0, "maximum", 100, "description", description);
    }
}
