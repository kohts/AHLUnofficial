package com.unofficial.ahl.model

import com.google.gson.annotations.SerializedName

/**
 * Main response structure from the Hebrew Academy AJAX API
 * Contains detailed word information including etymology, historical data, and related content
 */
data class DafMilaResponse(
    @SerializedName("ReuGam")
    val reuGam: String?,
    
    @SerializedName("Koteret")
    val koteret: String?,
    
    @SerializedName("MillonHaHoveList")
    val millonHaHoveList: List<MillonHaHoveItem>?,
    
    @SerializedName("ErekhHismagList")
    val erekhHismagList: List<ErekhHismagItem>?,
    
    @SerializedName("HalufonList")
    val halufonList: List<Any>?, // Empty array in sample, keeping as flexible type
    
    @SerializedName("MunnahimList")
    val munnahimList: MunnahimList?,
    
    @SerializedName("IndexName")
    val indexName: String?,
    
    @SerializedName("related_posts")
    val relatedPosts: List<RelatedPost>?,
    
    @SerializedName("related_hahlata")
    val relatedHahlata: List<Any>?, // Empty array in sample, keeping as flexible type
    
    @SerializedName("acf_categories")
    val acfCategories: Any? // null in sample, keeping as flexible type
)

/**
 * Individual dictionary entry from MillonHaHoveList
 */
data class MillonHaHoveItem(
    @SerializedName("HelekDibberTtl")
    val helekDibberTtl: String?,
    
    @SerializedName("HelekDibberText")
    val helekDibberText: String?,
    
    @SerializedName("NetiyyotTtl")
    val netiyyotTtl: String?,
    
    @SerializedName("NetiyyotPoalTtl")
    val netiyyotPoalTtl: String?,
    
    @SerializedName("NetiyyotTxt")
    val netiyyotTxt: String?,
    
    @SerializedName("NetiyyotPoalTxt")
    val netiyyotPoalTxt: String?,
    
    @SerializedName("PoalTtl")
    val poalTtl: String?,
    
    @SerializedName("PoalTxt")
    val poalTxt: String?,
    
    @SerializedName("ShemTtl")
    val shemTtl: String?,
    
    @SerializedName("ShemTxt")
    val shemTxt: String?,
    
    @SerializedName("ShoreshTxt")
    val shoreshTxt: String?,
    
    @SerializedName("ShoreshTtl")
    val shoreshTtl: String?,
    
    @SerializedName("Id")
    val id: Int,
    
    @SerializedName("Shoresh")
    val shoresh: String?,
    
    @SerializedName("IsTseruf")
    val isTseruf: Boolean,
    
    @SerializedName("Menukkad")
    val menukkad: String?,
    
    @SerializedName("HelekDibburTxt")
    val helekDibburTxt: String?,
    
    @SerializedName("MenukkadMispar")
    val menukkadMispar: String?,
    
    @SerializedName("KtivMale")
    val ktivMale: String?,
    
    @SerializedName("Nekeva")
    val nekeva: String?,
    
    @SerializedName("HearatTeken")
    val hearatTeken: String?,
    
    @SerializedName("HearatGizaron")
    val hearatGizaron: String?,
    
    @SerializedName("SafaTxt")
    val safaTxt: String?,
    
    @SerializedName("BinyanTxt")
    val binyanTxt: String?,
    
    @SerializedName("UrlNetyyot")
    val urlNetyyot: String?,
    
    @SerializedName("UrlNetyyotShem")
    val urlNetyyotShem: String?,
    
    @SerializedName("KoteretMilla")
    val koteretMilla: List<Any>?, // Empty array in sample
    
    @SerializedName("Hagdara")
    val hagdara: String?,
    
    @SerializedName("Tserufim")
    val tserufim: List<TserufItem>?,
    
    @SerializedName("MinDikdukiTxt")
    val minDikdukiTxt: String?,
    
    @SerializedName("MikumShema")
    val mikumShema: String?,
    
    @SerializedName("TxtTetsura")
    val txtTetsura: String?,
    
    @SerializedName("TeurSugeTetsura")
    val teurSugeTetsura: String?,
    
    @SerializedName("TxtTetsuranosefet")
    val txtTetsuranosefet: String?,
    
    @SerializedName("TeurSugeTetsuranosefet")
    val teurSugeTetsuranosefet: String?,
    
    @SerializedName("HearatTetsura")
    val hearatTetsura: String?,
    
    @SerializedName("Info")
    val info: String?
)

/**
 * Historical reference entry from ErekhHismagList
 */
data class ErekhHismagItem(
    @SerializedName("Id")
    val id: Int,
    
    @SerializedName("ShoreshId")
    val shoreshId: Int,
    
    @SerializedName("Shoresh")
    val shoresh: String?,
    
    @SerializedName("Menukad")
    val menukad: String?,
    
    @SerializedName("MenukadLoLetezuga")
    val menukadLoLetezuga: String?,
    
    @SerializedName("CalcErekh")
    val calcErekh: String?,
    
    @SerializedName("StrUrl")
    val strUrl: String?,
    
    @SerializedName("Type")
    val type: Int,
    
    @SerializedName("MeaningIndex")
    val meaningIndex: String?,
    
    @SerializedName("Meaning")
    val meaning: String?,
    
    @SerializedName("Binyan")
    val binyan: Int,
    
    @SerializedName("StrBinyan")
    val strBinyan: String?,
    
    @SerializedName("Reshima")
    val reshima: Int,
    
    @SerializedName("TotalCount")
    val totalCount: Int,
    
    @SerializedName("AvarNistar")
    val avarNistar: String?,
    
    @SerializedName("AvarNistarWithPe")
    val avarNistarWithPe: String?,
    
    @SerializedName("Tkufot")
    val tkufot: List<TkufaItem>?,
    
    @SerializedName("Info")
    val info: String?
)

/**
 * Related post/article information
 */
data class RelatedPost(
    @SerializedName("ID")
    val id: Int,
    
    @SerializedName("title")
    val title: String?,
    
    @SerializedName("permalink")
    val permalink: String?,
    
    @SerializedName("excerpt")
    val excerpt: String?,
    
    @SerializedName("match_type")
    val matchType: String?,
    
    @SerializedName("thumbnail")
    val thumbnail: String?
)

/**
 * Compound word/phrase entry from Tserufim list
 */
data class TserufItem(
    @SerializedName("KodErekh")
    val kodErekh: Int,
    
    @SerializedName("Menukad")
    val menukad: String?,
    
    @SerializedName("KtivMale")
    val ktivMale: String?,
    
    @SerializedName("Hagdara")
    val hagdara: String?
)

/**
 * Historical period usage data from Tkufot list
 */
data class TkufaItem(
    @SerializedName("Shkhihut")
    val shkhihut: Double,
    
    @SerializedName("Thilat_Tkufa")
    val thilatTkufa: String?,
    
    @SerializedName("Sof_Tkufa")
    val sofTkufa: String?
) 

/**
 * Munnah (terminology) list information
 */
data class MunnahimList(
    @SerializedName("ReshimaMelleha")
    val reshimaMelleha: String?,
    
    @SerializedName("KoteretMaagarMunnahim")
    val koteretMaagarMunnahim: String?,
    
    @SerializedName("KtaimHtml")
    val ktaimHtml: List<KetaHtml>?,
    
    @SerializedName("Info")
    val info: String?
)

/**
 * HTML entry item in KtaimHtml list
 */
data class KetaHtml(
    @SerializedName("DataKodKeta")
    val dataKodKeta: Int,
    
    @SerializedName("KetaNose")
    val ketaNose: String?,
    
    @SerializedName("HearaLeHatsaga")
    val hearaLeHatsaga: String?,
    
    @SerializedName("KetaPilluahLeMillon")
    val ketaPilluahLeMillon: List<PilluahLeMillon>?,
    
    @SerializedName("Halakim")
    val halakim: List<Helek>?
)

/**
 * Dictionary reference entry
 */
data class PilluahLeMillon(
    @SerializedName("href")
    val href: String?,
    
    @SerializedName("txt")
    val txt: String?
)

/**
 * Part/section information in Halakim list
 */
data class Helek(
    @SerializedName("Nirdafim")
    val nirdafim: String?,
    
    @SerializedName("HearatMesumman")
    val hearatMesumman: String?,
    
    @SerializedName("Mesummanim")
    val mesummanim: List<Any>?,
    
    @SerializedName("HelekHesber")
    val helekHesber: String?,
    
    @SerializedName("KvutsaHesber")
    val kvutsaHesber: String?,
    
    @SerializedName("NirdafHesber")
    val nirdafHesber: String?,
    
    @SerializedName("NirdafLink")
    val nirdafLink: NirdafLink?
)

/**
 * Link information for synonyms
 */
data class NirdafLink(
    @SerializedName("href")
    val href: String?,
    
    @SerializedName("txt")
    val txt: String?,
    
    @SerializedName("safa")
    val safa: String?
) 