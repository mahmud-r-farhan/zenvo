package com.bengalbytes.zenvo.data

data class Hadith(
    val text: String,
    val reference: String
)

object IslamicContent {
    val HADITH_LIST = listOf(
        Hadith(
            "Take advantage of five before five: your youth before your old age, your health before your illness, your wealth before your poverty, your free time before your busy-ness, and your life before your death.",
            "Shu'ab al-Iman 9767"
        ),
        Hadith(
            "There are two blessings which many people lose: (They are) Health and free time for doing good.",
            "Sahih Bukhari 6412"
        ),
        Hadith(
            "Allah loves that when one of you does a job, he does it perfectly (itqan).",
            "Al-Mu'jam al-Awsat 897"
        ),
        Hadith(
            "The most beloved of deeds to Allah are those that are most consistent, even if they are small.",
            "Sahih Bukhari 6464"
        ),
        Hadith(
            "A strong believer is better and more beloved to Allah than a weak believer, though there is good in both. Be eager for what benefits you.",
            "Sahih Muslim 2664"
        ),
        Hadith(
            "Allah's Messenger (peace be upon him) said: 'The feet of a slave will not move on the Day of Judgment until he is asked about four things: about his life and how he spent it, about his knowledge and what he did with it...'",
            "Tirmidhi 2417"
        ),
        Hadith(
            "The Prophet (peace be upon him) said: 'Make things easy and do not make them difficult, cheer the people up and do not make them run away.'",
            "Sahih Bukhari 69"
        ),
        Hadith(
            "The Prophet (peace be upon him) said: 'Blessed is he whose own faults keep him from seeing the faults of others.'",
            "Al-Bazzar"
        ),
        Hadith(
            "The Prophet (peace be upon him) said: 'Whosoever follows a path to seek knowledge therein, Allah will make easy for him a path to Paradise.'",
            "Sahih Muslim 2699"
        ),
        Hadith(
            "The Prophet (peace be upon him) said: 'None of you truly believes until he loves for his brother what he loves for himself.'",
            "Sahih Bukhari 13"
        )
    )

    /**
     * Returns a Hadith index based on the current hour to provide variety over time.
     */
    fun getHadithIndexByTime(): Int {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return hour % HADITH_LIST.size
    }
}
