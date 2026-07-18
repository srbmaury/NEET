package com.neet.app.domain

import com.neet.app.data.model.Subject

/**
 * Approximate NEET exam weightage per topic, based on well-established previous-year
 * question-distribution patterns (qualitative tiers, not exact counts — the exact number of
 * questions per topic varies slightly year to year, but the relative importance is stable).
 * Used to prioritize weak/unpracticed topics that are actually worth studying first.
 */
enum class Weightage { HIGH, MEDIUM, LOW }

private data class TopicEntry(val name: String, val weightage: Weightage)

object TopicCatalog {

    private val topics: Map<Subject, List<TopicEntry>> = mapOf(
        Subject.PHYSICS to listOf(
            TopicEntry("Physical World and Measurement", Weightage.LOW),
            TopicEntry("Kinematics", Weightage.HIGH),
            TopicEntry("Laws of Motion", Weightage.HIGH),
            TopicEntry("Work, Energy and Power", Weightage.HIGH),
            TopicEntry("System of Particles and Rotational Motion", Weightage.HIGH),
            TopicEntry("Gravitation", Weightage.MEDIUM),
            TopicEntry("Mechanical Properties of Solids", Weightage.LOW),
            TopicEntry("Mechanical Properties of Fluids", Weightage.MEDIUM),
            TopicEntry("Thermal Properties of Matter", Weightage.MEDIUM),
            TopicEntry("Thermodynamics", Weightage.MEDIUM),
            TopicEntry("Kinetic Theory of Gases", Weightage.LOW),
            TopicEntry("Oscillations", Weightage.MEDIUM),
            TopicEntry("Waves", Weightage.MEDIUM),
            TopicEntry("Electrostatics", Weightage.HIGH),
            TopicEntry("Current Electricity", Weightage.HIGH),
            TopicEntry("Magnetic Effects of Current and Magnetism", Weightage.HIGH),
            TopicEntry("Electromagnetic Induction and Alternating Currents", Weightage.MEDIUM),
            TopicEntry("Electromagnetic Waves", Weightage.LOW),
            TopicEntry("Ray Optics and Optical Instruments", Weightage.HIGH),
            TopicEntry("Wave Optics", Weightage.MEDIUM),
            TopicEntry("Dual Nature of Matter and Radiation", Weightage.MEDIUM),
            TopicEntry("Atoms and Nuclei", Weightage.HIGH),
            TopicEntry("Semiconductor Electronics", Weightage.MEDIUM),
        ),
        Subject.CHEMISTRY to listOf(
            TopicEntry("Some Basic Concepts in Chemistry", Weightage.LOW),
            TopicEntry("Structure of Atom", Weightage.MEDIUM),
            TopicEntry("Chemical Bonding and Molecular Structure", Weightage.HIGH),
            TopicEntry("States of Matter", Weightage.LOW),
            TopicEntry("Chemical Thermodynamics", Weightage.MEDIUM),
            TopicEntry("Equilibrium", Weightage.HIGH),
            TopicEntry("Redox Reactions", Weightage.MEDIUM),
            TopicEntry("Solutions", Weightage.MEDIUM),
            TopicEntry("Electrochemistry", Weightage.HIGH),
            TopicEntry("Chemical Kinetics", Weightage.MEDIUM),
            TopicEntry("Classification of Elements and Periodicity", Weightage.LOW),
            TopicEntry("Hydrogen", Weightage.LOW),
            TopicEntry("s-Block Elements", Weightage.LOW),
            TopicEntry("p-Block Elements", Weightage.HIGH),
            TopicEntry("d- and f-Block Elements", Weightage.MEDIUM),
            TopicEntry("Coordination Compounds", Weightage.HIGH),
            TopicEntry("Basic Principles and Techniques of Organic Chemistry", Weightage.HIGH),
            TopicEntry("Hydrocarbons", Weightage.HIGH),
            TopicEntry("Organic Compounds Containing Halogens", Weightage.MEDIUM),
            TopicEntry("Organic Compounds Containing Oxygen", Weightage.HIGH),
            TopicEntry("Organic Compounds Containing Nitrogen", Weightage.MEDIUM),
            TopicEntry("Biomolecules", Weightage.HIGH),
            TopicEntry("Polymers", Weightage.LOW),
            TopicEntry("Chemistry in Everyday Life", Weightage.LOW),
        ),
        Subject.BOTANY to listOf(
            TopicEntry("The Living World", Weightage.LOW),
            TopicEntry("Biological Classification", Weightage.MEDIUM),
            TopicEntry("Plant Kingdom", Weightage.MEDIUM),
            TopicEntry("Morphology of Flowering Plants", Weightage.MEDIUM),
            TopicEntry("Anatomy of Flowering Plants", Weightage.MEDIUM),
            TopicEntry("Cell: The Unit of Life", Weightage.HIGH),
            TopicEntry("Biomolecules", Weightage.HIGH),
            TopicEntry("Cell Cycle and Cell Division", Weightage.HIGH),
            TopicEntry("Transport in Plants", Weightage.MEDIUM),
            TopicEntry("Mineral Nutrition", Weightage.MEDIUM),
            TopicEntry("Photosynthesis in Higher Plants", Weightage.HIGH),
            TopicEntry("Respiration in Plants", Weightage.HIGH),
            TopicEntry("Plant Growth and Development", Weightage.MEDIUM),
            TopicEntry("Sexual Reproduction in Flowering Plants", Weightage.HIGH),
            TopicEntry("Principles of Inheritance and Variation", Weightage.HIGH),
            TopicEntry("Molecular Basis of Inheritance", Weightage.HIGH),
            TopicEntry("Evolution", Weightage.MEDIUM),
            TopicEntry("Biotechnology: Principles and Processes", Weightage.HIGH),
            TopicEntry("Biotechnology and its Applications", Weightage.MEDIUM),
            TopicEntry("Organisms and Populations", Weightage.HIGH),
            TopicEntry("Ecosystem", Weightage.MEDIUM),
            TopicEntry("Biodiversity and Conservation", Weightage.MEDIUM),
            TopicEntry("Environmental Issues", Weightage.LOW),
        ),
        Subject.ZOOLOGY to listOf(
            TopicEntry("Animal Kingdom", Weightage.HIGH),
            TopicEntry("Structural Organisation in Animals", Weightage.MEDIUM),
            TopicEntry("Digestion and Absorption", Weightage.HIGH),
            TopicEntry("Breathing and Exchange of Gases", Weightage.HIGH),
            TopicEntry("Body Fluids and Circulation", Weightage.HIGH),
            TopicEntry("Excretory Products and their Elimination", Weightage.HIGH),
            TopicEntry("Locomotion and Movement", Weightage.MEDIUM),
            TopicEntry("Neural Control and Coordination", Weightage.HIGH),
            TopicEntry("Chemical Coordination and Integration", Weightage.HIGH),
            TopicEntry("Human Reproduction", Weightage.HIGH),
            TopicEntry("Reproductive Health", Weightage.MEDIUM),
            TopicEntry("Human Health and Disease", Weightage.HIGH),
            TopicEntry("Strategies for Enhancement in Food Production", Weightage.LOW),
            TopicEntry("Microbes in Human Welfare", Weightage.MEDIUM),
        ),
    )

    fun topicsFor(subject: Subject): List<String> = topics.getValue(subject).map { it.name }

    fun weightageOf(subject: Subject, topic: String): Weightage =
        topics.getValue(subject).firstOrNull { it.name == topic }?.weightage ?: Weightage.MEDIUM

    fun allTopics(): List<Pair<Subject, String>> =
        topics.flatMap { (subject, entries) -> entries.map { subject to it.name } }
}
