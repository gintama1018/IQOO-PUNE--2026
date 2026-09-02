package com.skilllens.app.taskengine

// ─────────────────────────────────────────────────────────────────────────────
// Skill Repository — Built-in skill definitions for MVP
//
// MVP: One electrical wiring task (safe low-voltage training board).
// Architecture: New skills can be added here or loaded from assets/JSON.
// The engine code does NOT need to change when a new skill is added.
// ─────────────────────────────────────────────────────────────────────────────

object SkillRepository {

    fun getAllSkills(): List<SkillDefinition> = listOf(electricalWiringSkill())

    fun getSkillById(id: String): SkillDefinition? =
        getAllSkills().find { it.id == id }

    // ─────────────────────────────────────────────────────────────────────────
    // SKILL: Basic Electrical Circuit Wiring
    // Training board: L, N, E terminals + MCB + socket
    // All work MUST be done on a SAFE LOW-VOLTAGE TRAINING BOARD ONLY.
    // ─────────────────────────────────────────────────────────────────────────
    private fun electricalWiringSkill() = SkillDefinition(
        id          = "electrical_wiring_basic",
        name        = "Basic Circuit Wiring",
        version     = "1.0.0",
        description = "Wire a basic single-phase circuit on a training board: " +
                "connect Live (L), Neutral (N), and Earth (E) wires to the correct terminals.",
        safetyNote  = "⚡ SAFETY: Use ONLY a safe low-voltage educational training board. " +
                "Never perform this on live mains electricity. SkillLens is a training aid only.",
        estimatedDurationMin = 5,
        requiredEquipment = listOf(
            "Low-voltage electrical training board",
            "Red wire (Live)",
            "Black wire (Neutral)",
            "Green/Yellow wire (Earth)",
            "Screwdriver",
        ),
        steps = listOf(
            TaskStep(
                id           = "step_1_identify",
                title        = "Identify Components",
                instruction  = "Locate the L, N, and E terminal blocks on the board.",
                requiredObjects       = listOf("board", "terminal"),
                requiredRelationships = emptyList(),
                successState = TaskState.STEP_2_PICK_WIRE.name,
                errorStates  = listOf("wrong_component", "out_of_order"),
                timeoutMs    = 20_000L,
                minConfidence = 0.55f,
                debounceFrames = 3,
            ),
            TaskStep(
                id           = "step_2_pick_wire",
                title        = "Pick Up Live Wire",
                instruction  = "Pick up the RED wire (Live / L terminal).",
                requiredObjects       = listOf("red_wire"),
                requiredRelationships = listOf("hand_holding_red_wire"),
                successState  = TaskState.STEP_3_CONNECT_L.name,
                errorStates   = listOf("wrong_component", "out_of_order"),
                timeoutMs     = 25_000L,
                minConfidence = 0.60f,
                debounceFrames = 4,
            ),
            TaskStep(
                id           = "step_3_connect_L",
                title        = "Connect Live (L)",
                instruction  = "Insert the RED wire into the L terminal and tighten the screw.",
                requiredObjects       = listOf("red_wire", "l_terminal"),
                requiredRelationships = listOf("red_wire_connected_to_l"),
                successState  = TaskState.STEP_4_CONNECT_N.name,
                errorStates   = listOf("wrong_connection", "wrong_component", "out_of_order"),
                timeoutMs     = 30_000L,
                minConfidence = 0.65f,
                debounceFrames = 5,
            ),
            TaskStep(
                id           = "step_4_connect_N",
                title        = "Connect Neutral (N)",
                instruction  = "Insert the BLACK wire into the N terminal and tighten the screw.",
                requiredObjects       = listOf("black_wire", "n_terminal"),
                requiredRelationships = listOf("black_wire_connected_to_n"),
                successState  = TaskState.STEP_5_CONNECT_E.name,
                errorStates   = listOf("wrong_connection", "wrong_component", "out_of_order"),
                timeoutMs     = 30_000L,
                minConfidence = 0.65f,
                debounceFrames = 5,
            ),
            TaskStep(
                id           = "step_5_connect_E",
                title        = "Connect Earth (E)",
                instruction  = "Insert the GREEN/YELLOW wire into the E terminal and tighten the screw.",
                requiredObjects       = listOf("earth_wire", "e_terminal"),
                requiredRelationships = listOf("earth_wire_connected_to_e"),
                successState  = TaskState.STEP_6_VERIFY.name,
                errorStates   = listOf("wrong_connection", "wrong_component", "out_of_order"),
                timeoutMs     = 30_000L,
                minConfidence = 0.65f,
                debounceFrames = 5,
            ),
            TaskStep(
                id           = "step_6_verify",
                title        = "Final Verification",
                instruction  = "Hold phone steady. Verifying all connections...",
                requiredObjects = listOf(
                    "red_wire", "black_wire", "earth_wire",
                    "l_terminal", "n_terminal", "e_terminal"
                ),
                requiredRelationships = listOf(
                    "red_wire_connected_to_l",
                    "black_wire_connected_to_n",
                    "earth_wire_connected_to_e",
                ),
                successState  = TaskState.COMPLETED.name,
                errorStates   = listOf("wrong_connection", "missing_component"),
                timeoutMs     = 20_000L,
                minConfidence = 0.70f,
                debounceFrames = 6,
            ),
        ),
        scoringWeights = ScoringWeights(
            completionWeight = 0.40f,
            accuracyWeight   = 0.35f,
            sequenceWeight   = 0.15f,
            speedWeight      = 0.10f,
        ),
    )
}
