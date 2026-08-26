package me.sandbad.medireminder.core.model

/** Physical form of a medication — drives the icon and the "unit" wording. */
enum class MedicationForm(val label: String, val unitLabel: String) {
    TABLET("Tablet", "tablet"),
    CAPSULE("Capsule", "capsule"),
    LIQUID("Liquid", "ml"),
    INJECTION("Injection", "shot"),
    DROPS("Drops", "drop"),
    INHALER("Inhaler", "puff"),
    PATCH("Patch", "patch"),
    CREAM("Cream", "application"),
    OTHER("Other", "dose")
}

enum class StrengthUnit(val label: String) {
    MG("mg"), MCG("mcg"), G("g"), ML("ml"), IU("IU"), PERCENT("%"), NONE("")
}

/** How the doses of a medication repeat. */
enum class ScheduleType(val label: String) {
    /** Every day, at each time in [Schedule.timesOfDay]. */
    DAILY("Every day"),

    /** Only on the ISO weekdays listed in [Schedule.daysOfWeek]. */
    SPECIFIC_DAYS("Specific days"),

    /** Every N days counting from the medication start date. */
    INTERVAL_DAYS("Every N days"),

    /** No automatic reminders — the user logs doses manually. */
    AS_NEEDED("As needed")
}

enum class DoseStatus(val label: String) {
    PENDING("Pending"),
    TAKEN("Taken"),
    SKIPPED("Skipped"),
    MISSED("Missed")
}

/** Fixed palette so a medication is recognisable at a glance in lists and history. */
enum class MedColor(val hex: Long) {
    BLUE(0xFF3B82F6),
    TEAL(0xFF14B8A6),
    GREEN(0xFF22C55E),
    AMBER(0xFFF59E0B),
    ORANGE(0xFFF97316),
    ROSE(0xFFF43F5E),
    PURPLE(0xFF8B5CF6),
    SLATE(0xFF64748B);

    companion object {
        fun fromName(value: String?): MedColor =
            entries.firstOrNull { it.name == value } ?: BLUE
    }
}
