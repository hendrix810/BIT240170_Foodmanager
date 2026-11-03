package com.myapplication

import android.database.sqlite.SQLiteException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.LruCache
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

// =====================================================================
// ĐA NGÔN NGỮ: mặc định English, chuyển được sang Tiếng Việt.
// Mỗi mục: "khóa" to ("English" to "Tiếng Việt")
// =====================================================================
object Lang {
    var code = "en"

    private val S: Map<String, Pair<String, String>> = mapOf(
        "add_dish" to ("Add dish" to "Thêm món"),
        "categories" to ("Categories" to "Loại món"),
        "search_hint" to ("Search by name" to "Tìm theo tên món"),
        "all_categories" to ("All categories" to "Tất cả loại món"),
        "only_available" to ("Available only" to "Chỉ món đang bán"),
        "min_price" to ("Min price" to "Giá từ"),
        "max_price" to ("Max price" to "Giá đến"),
        "sort_none" to ("No sorting" to "Không sắp xếp"),
        "sort_price_asc" to ("Price: low to high" to "Giá tăng dần"),
        "sort_price_desc" to ("Price: high to low" to "Giá giảm dần"),
        "sort_time_asc" to ("Preparation time: shortest first" to "Thời gian chế biến tăng dần"),
        "err_price_range" to ("Invalid price range" to "Khoảng giá không hợp lệ"),
        "no_dishes" to ("No dishes found" to "Không có món ăn nào"),
        "quick" to ("Quick" to "Nhanh"),
        "regular" to ("Regular" to "Thông thường"),
        "min" to ("min" to "phút"),
        "available" to ("Available" to "Đang bán"),
        "unavailable" to ("Unavailable" to "Ngừng bán"),
        "price" to ("Price" to "Giá"),
        "prep_time" to ("Preparation time" to "Thời gian chế biến"),
        "prep_time_hint" to ("Preparation time (minutes)" to "Thời gian chế biến (phút)"),
        "category" to ("Category" to "Loại món"),
        "description" to ("Description" to "Mô tả"),
        "name" to ("Name" to "Tên món"),
        "images" to ("Images" to "Hình ảnh"),
        "no_images" to ("No images" to "Chưa có ảnh"),
        "thumbnail" to ("Thumbnail" to "Ảnh đại diện"),
        "set_thumbnail" to ("Set as thumbnail" to "Đặt làm ảnh đại diện"),
        "image_url" to ("Image URL" to "Đường dẫn ảnh"),
        "add_image" to ("Add image" to "Thêm ảnh"),
        "edit" to ("Edit" to "Sửa"),
        "delete" to ("Delete" to "Xóa"),
        "back" to ("Back" to "Quay lại"),
        "save" to ("Save" to "Lưu"),
        "cancel" to ("Cancel" to "Hủy"),
        "confirm_delete_dish" to ("Delete this dish?" to "Bạn có chắc muốn xóa món này?"),
        "confirm_delete_category" to ("Delete this category?" to "Bạn có chắc muốn xóa loại món này?"),
        "confirm_delete_image" to ("Delete this image?" to "Bạn có chắc muốn xóa ảnh này?"),
        "add_dish_title" to ("Add dish" to "Thêm món ăn"),
        "edit_dish_title" to ("Edit dish" to "Chỉnh sửa món ăn"),
        "first_image" to ("First image URL (optional)" to "Đường dẫn ảnh đầu tiên (không bắt buộc)"),
        "saved" to ("Saved" to "Đã lưu"),
        "deleted" to ("Deleted" to "Đã xóa"),
        "err_name_empty" to ("Name must not be empty" to "Tên không được để trống"),
        "err_price" to ("Price must be greater than 0" to "Giá phải lớn hơn 0"),
        "err_time" to ("Preparation time must be greater than 0" to "Thời gian chế biến phải lớn hơn 0"),
        "err_category" to ("Please choose a valid category" to "Vui lòng chọn loại món hợp lệ"),
        "err_duplicate" to ("This dish name already exists in the category" to "Tên món đã tồn tại trong loại này"),
        "err_not_found" to ("Item not found" to "Không tìm thấy dữ liệu"),
        "err_category_in_use" to ("Cannot delete: the category is in use" to "Không thể xóa: loại món đang được sử dụng"),
        "err_url" to ("Image URL must start with http:// or https://" to "Đường dẫn ảnh phải bắt đầu bằng http:// hoặc https://"),
        "err_db" to ("Database error, please try again" to "Lỗi truy cập dữ liệu, vui lòng thử lại"),
        "no_categories" to ("No categories yet. Add a category first." to "Chưa có loại món. Hãy thêm loại món trước."),
        "no_categories_list" to ("No categories" to "Chưa có loại món"),
        "category_name" to ("Category name" to "Tên loại món"),
        "add_category" to ("Add category" to "Thêm loại món"),
        "edit_category" to ("Edit category" to "Sửa loại món")
    )

    fun t(key: String): String =
        S[key]?.let { if (code == "vi") it.second else it.first } ?: key
}

private fun t(key: String) = Lang.t(key)

// =====================================================================
// Tải ảnh từ ImageUrl (không dùng thư viện ngoài). Cần quyền INTERNET trong AndroidManifest.
// =====================================================================
object ImageLoader {
    private val cache = LruCache<String, Bitmap>(40)
    private val executor = Executors.newFixedThreadPool(3)
    private val main = Handler(Looper.getMainLooper())

    fun load(url: String, view: ImageView) {
        view.tag = url
        view.setImageResource(android.R.drawable.ic_menu_gallery)
        cache.get(url)?.let { view.setImageBitmap(it); return }

        executor.execute {
            val bmp: Bitmap? = try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                val bytes = conn.inputStream.use { it.readBytes() }
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                var sample = 1
                while (bounds.outWidth / sample > 800 || bounds.outHeight / sample > 800) sample *= 2
                BitmapFactory.decodeByteArray(
                    bytes, 0, bytes.size,
                    BitmapFactory.Options().apply { inSampleSize = sample }
                )
            } catch (e: Exception) {
                null
            }
            if (bmp != null) cache.put(url, bmp)
            main.post {
                if (view.tag == url) {
                    if (bmp != null) view.setImageBitmap(bmp)
                    else view.setImageResource(android.R.drawable.ic_delete)   // URL lỗi
                }
            }
        }
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var container: FrameLayout

    private var filter = DishFilter()          // giữ điều kiện lọc/sắp xếp khi quay về danh sách
    private var render: () -> Unit = {}        // vẽ lại màn hình hiện tại (khi đổi ngôn ngữ)
    private var onBack: (() -> Unit)? = null   // null = đang ở màn hình danh sách

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = AppDatabase.getInstance(this)

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        Lang.code = prefs.getString("lang", "en") ?: "en"      // mặc định English, nhớ lựa chọn

        // ---- Khung chung: thanh tiêu đề (có MSSV) + nút đổi ngôn ngữ + vùng nội dung ----
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val header = hbox().apply {
            setBackgroundColor(0xFF1565C0.toInt())
            setPadding(dp(12), dp(8), dp(8), dp(8))
        }
        header.addView(
            label("${MSSV}_FoodManager", 18f, true, Color.WHITE),
            LinearLayout.LayoutParams(0, WRAP, 1f)
        )
        val btnLang = button("") {}
        fun updateLangButton() { btnLang.text = if (Lang.code == "en") "Tiếng Việt" else "English" }
        updateLangButton()
        btnLang.setOnClickListener {
            Lang.code = if (Lang.code == "en") "vi" else "en"
            prefs.edit().putString("lang", Lang.code).apply()
            updateLangButton()
            render()                                            // giữ ngôn ngữ khi chuyển màn hình
        }
        header.addView(btnLang)
        root.addView(header, LinearLayout.LayoutParams(MATCH, WRAP))

        container = FrameLayout(this)
        root.addView(container, LinearLayout.LayoutParams(MATCH, 0, 1f))
        setContentView(root)

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val b = onBack
                if (b != null) b() else finish()
            }
        })

        showList()
    }

    // =================================================================
    // MÀN HÌNH DANH SÁCH: tìm kiếm, lọc nhiều điều kiện, sắp xếp
    // =================================================================
    private fun showList() {
        onBack = null
        render = { showList() }
        val categories = safe { db.getCategories() } ?: emptyList()

        val box = vbox()

        val top = hbox()
        top.addView(button(t("add_dish")) { showForm(null) }, LinearLayout.LayoutParams(0, WRAP, 1f))
        top.addView(button(t("categories")) { showCategories() }, LinearLayout.LayoutParams(0, WRAP, 1f))
        box.addView(top)

        val etSearch = input(t("search_hint"), InputType.TYPE_CLASS_TEXT, filter.keyword)
        val spCat = spinner(
            listOf(t("all_categories")) + categories.map { it.name },
            categories.indexOfFirst { it.id == filter.categoryId } + 1   // -1 + 1 = 0 (tất cả)
        )
        val cbAvail = CheckBox(this).apply {
            text = t("only_available")
            isChecked = filter.onlyAvailable
        }
        val numType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        val etMin = input(t("min_price"), numType, filter.minPrice?.let { num(it) } ?: "")
        val etMax = input(t("max_price"), numType, filter.maxPrice?.let { num(it) } ?: "")
        val sortKeys = listOf(
            SortOption.NONE, SortOption.PRICE_ASC, SortOption.PRICE_DESC, SortOption.TIME_ASC
        )
        val spSort = spinner(
            listOf(t("sort_none"), t("sort_price_asc"), t("sort_price_desc"), t("sort_time_asc")),
            sortKeys.indexOf(filter.sort)
        )
        val tvErr = label(t("err_price_range"), 13f, false, Color.RED).apply { visibility = View.GONE }
        val listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        box.addView(etSearch, lp(4))
        box.addView(spCat)
        box.addView(cbAvail)
        val priceRow = hbox()
        priceRow.addView(etMin, LinearLayout.LayoutParams(0, WRAP, 1f))
        priceRow.addView(etMax, LinearLayout.LayoutParams(0, WRAP, 1f))
        box.addView(priceRow)
        box.addView(tvErr)
        box.addView(spSort)
        box.addView(divider(), lp(8))
        box.addView(listBox)

        fun refresh() {
            val minTxt = etMin.text.toString().trim()
            val maxTxt = etMax.text.toString().trim()
            val minP = minTxt.toDoubleOrNull()
            val maxP = maxTxt.toDoubleOrNull()
            val badNumber = (minTxt.isNotEmpty() && minP == null) || (maxTxt.isNotEmpty() && maxP == null)

            filter = DishFilter(
                keyword = etSearch.text.toString(),
                categoryId = categories.getOrNull(spCat.selectedItemPosition - 1)?.id,
                onlyAvailable = cbAvail.isChecked,
                minPrice = minP,
                maxPrice = maxP,
                sort = sortKeys[spSort.selectedItemPosition]
            )

            listBox.removeAllViews()
            if (badNumber || !filter.isPriceRangeValid) {        // khoảng giá không hợp lệ
                tvErr.visibility = View.VISIBLE
                return
            }
            tvErr.visibility = View.GONE

            val dishes = safe { db.getDishes(filter) } ?: return
            if (dishes.isEmpty()) {                               // danh sách trống
                listBox.addView(label(t("no_dishes"), 16f), lp(16))
                return
            }
            val names = categories.associate { it.id to it.name }
            dishes.forEach {
                listBox.addView(dishRow(it, names[it.dishCategoryId] ?: "?"))
                listBox.addView(divider())
            }
        }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) { refresh() }
        }
        etSearch.addTextChangedListener(watcher)
        etMin.addTextChangedListener(watcher)
        etMax.addTextChangedListener(watcher)

        val selListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) { refresh() }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        spCat.onItemSelectedListener = selListener
        spSort.onItemSelectedListener = selListener
        cbAvail.setOnCheckedChangeListener { _, _ -> refresh() }

        setScreen(box)
        refresh()
    }

    private fun dishRow(d: Dish, catName: String): View {
        val row = hbox().apply {
            setPadding(0, dp(8), 0, dp(8))
            isClickable = true
            setOnClickListener { showDetail(d.id) }
        }
        val iv = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        val url = d.thumbnailUrl
        if (url != null) ImageLoader.load(url, iv) else iv.setImageResource(android.R.drawable.ic_menu_gallery)
        row.addView(iv, LinearLayout.LayoutParams(dp(80), dp(80)))

        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        info.addView(label(d.name, 17f, true))
        info.addView(label(money(d.price), 15f))
        info.addView(label("$catName • ${d.preparationTime} ${t("min")} • ${speed(d)}", 13f))
        info.addView(
            label(
                t(if (d.isAvailable) "available" else "unavailable"), 13f, true,
                if (d.isAvailable) 0xFF2E7D32.toInt() else Color.RED
            )
        )
        row.addView(info, LinearLayout.LayoutParams(0, WRAP, 1f))
        return row
    }

    // =================================================================
    // MÀN HÌNH CHI TIẾT: nhiều ảnh, đổi ảnh đại diện, thêm ảnh, sửa, xóa
    // =================================================================
    private fun showDetail(id: Int) {
        val dish = safe { db.getDish(id) }
        if (dish == null) {                                       // Id không tồn tại
            toast(t("err_not_found"))
            showList()
            return
        }
        onBack = { showList() }
        render = { showDetail(id) }

        val catName = safe { db.getCategory(dish.dishCategoryId)?.name } ?: "?"
        val box = vbox()

        box.addView(label(dish.name, 22f, true))
        box.addView(label("${t("price")}: ${money(dish.price)}", 16f), lp(8))
        box.addView(
            label("${t("prep_time")}: ${dish.preparationTime} ${t("min")} (${speed(dish)})", 16f), lp(4)
        )
        box.addView(label("${t("category")}: $catName", 16f), lp(4))
        box.addView(
            label(
                t(if (dish.isAvailable) "available" else "unavailable"), 16f, true,
                if (dish.isAvailable) 0xFF2E7D32.toInt() else Color.RED
            ), lp(4)
        )
        if (dish.description.isNotBlank()) box.addView(label(dish.description, 15f), lp(8))

        // ---- Ảnh ----
        box.addView(label(t("images"), 18f, true), lp(16))
        if (dish.images.isEmpty()) {
            box.addView(label(t("no_images")), lp(4))                 // ảnh trống
        } else {
            val hs = HorizontalScrollView(this)
            val strip = hbox().apply { gravity = Gravity.TOP }
            dish.images.forEach { img ->
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, 0, dp(8), 0)
                }
                val iv = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
                ImageLoader.load(img.imageUrl, iv)
                card.addView(iv, LinearLayout.LayoutParams(dp(160), dp(120)))
                if (img.isThumbnail) {
                    card.addView(label("★ " + t("thumbnail"), 13f, true))
                } else {
                    card.addView(button(t("set_thumbnail")) {
                        safe { db.setThumbnail(img.id) }
                        showDetail(id)
                    })
                }
                card.addView(button(t("delete")) {
                    confirm(t("confirm_delete_image")) {
                        safe { db.deleteImage(img.id) }
                        showDetail(id)
                    }
                })
                strip.addView(card)
            }
            hs.addView(strip)
            box.addView(hs)
        }

        val etUrl = input(
            t("image_url"), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        )
        box.addView(etUrl, lp(8))
        box.addView(button(t("add_image")) {
            val url = etUrl.text.toString().trim()
            if (!isHttpUrl(url)) {
                etUrl.error = t("err_url")
                return@button
            }
            val newId = safe { db.addImage(id, url) } ?: return@button
            if (newId < 0) toast(t("err_not_found")) else showDetail(id)
        })

        // ---- Nút thao tác ----
        val actions = hbox()
        actions.addView(button(t("edit")) { showForm(id) }, LinearLayout.LayoutParams(0, WRAP, 1f))
        actions.addView(button(t("delete")) {
            confirm(t("confirm_delete_dish")) {                      // xóa có xác nhận
                if (safe { db.deleteDish(id) } == true) {
                    toast(t("deleted"))
                    showList()
                }
            }
        }, LinearLayout.LayoutParams(0, WRAP, 1f))
        actions.addView(button(t("back")) { showList() }, LinearLayout.LayoutParams(0, WRAP, 1f))
        box.addView(actions, lp(16))

        setScreen(box)
    }

    // =================================================================
    // FORM THÊM / SỬA MÓN
    // =================================================================
    private fun showForm(dishId: Int?) {
        val existing = if (dishId != null) safe { db.getDish(dishId) } else null
        if (dishId != null && existing == null) {
            toast(t("err_not_found"))
            showList()
            return
        }
        val categories = safe { db.getCategories() } ?: emptyList()
        onBack = { if (existing != null) showDetail(existing.id) else showList() }
        render = { showForm(dishId) }

        val box = vbox()
        box.addView(label(t(if (existing == null) "add_dish_title" else "edit_dish_title"), 20f, true))
        if (categories.isEmpty()) {
            box.addView(label(t("no_categories"), 14f, false, Color.RED), lp(8))
        }

        val etName = input(t("name"), InputType.TYPE_CLASS_TEXT, existing?.name ?: "")
        val etPrice = input(
            t("price"), InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
            existing?.let { num(it.price) } ?: ""
        )
        val etTime = input(
            t("prep_time_hint"), InputType.TYPE_CLASS_NUMBER,
            existing?.preparationTime?.toString() ?: ""
        )
        val etDesc = input(
            t("description"),
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
            existing?.description ?: ""
        )
        // Chọn loại món từ dữ liệu đã lưu
        val spCat = spinner(
            categories.map { it.name },
            categories.indexOfFirst { it.id == existing?.dishCategoryId }.coerceAtLeast(0)
        )
        val cbAvail = CheckBox(this).apply {
            text = t("available")
            isChecked = existing?.isAvailable ?: true
        }
        val etImg: EditText? = if (existing == null) input(
            t("first_image"), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        ) else null

        box.addView(label(t("name"), 13f, true), lp(10)); box.addView(etName)
        box.addView(label(t("price"), 13f, true), lp(6)); box.addView(etPrice)
        box.addView(label(t("prep_time"), 13f, true), lp(6)); box.addView(etTime)
        box.addView(label(t("category"), 13f, true), lp(6)); box.addView(spCat)
        box.addView(label(t("description"), 13f, true), lp(6)); box.addView(etDesc)
        box.addView(cbAvail, lp(6))
        if (etImg != null) box.addView(etImg)

        val actions = hbox()
        actions.addView(button(t("save")) {
            val dish = Dish(
                id = existing?.id ?: 0,
                name = etName.text.toString(),
                price = etPrice.text.toString().trim().toDoubleOrNull() ?: 0.0,
                preparationTime = etTime.text.toString().trim().toIntOrNull() ?: 0,
                isAvailable = cbAvail.isChecked,
                description = etDesc.text.toString(),
                dishCategoryId = categories.getOrNull(spCat.selectedItemPosition)?.id ?: -1
            )
            val url = etImg?.text?.toString()?.trim().orEmpty()

            try {
                val err = db.validateDish(dish)
                if (err != null) {
                    when (err) {
                        DishError.NAME_EMPTY -> etName.error = t("err_name_empty")
                        DishError.NAME_DUPLICATE -> etName.error = t("err_duplicate")
                        DishError.PRICE_INVALID -> etPrice.error = t("err_price")
                        DishError.TIME_INVALID -> etTime.error = t("err_time")
                        DishError.CATEGORY_INVALID -> toast(t("err_category"))
                    }
                    return@button
                }
                if (url.isNotEmpty() && !isHttpUrl(url)) {
                    etImg?.error = t("err_url")
                    return@button
                }

                val savedId: Int
                if (existing == null) {
                    val newId = db.insertDish(dish)
                    if (newId <= 0) {
                        toast(t("err_db"))
                        return@button
                    }
                    if (url.isNotEmpty()) db.addImage(newId.toInt(), url)
                    savedId = newId.toInt()
                } else {
                    db.updateDish(dish)
                    savedId = existing.id
                }
                toast(t("saved"))
                showDetail(savedId)                                 // hiển thị lại dữ liệu sau khi lưu
            } catch (e: SQLiteException) {
                toast(t("err_db"))
            }
        }, LinearLayout.LayoutParams(0, WRAP, 1f))
        actions.addView(button(t("cancel")) { onBack?.invoke() }, LinearLayout.LayoutParams(0, WRAP, 1f))
        box.addView(actions, lp(16))

        setScreen(box)
    }

    // =================================================================
    // QUẢN LÝ LOẠI MÓN (không cho xóa loại đang được sử dụng)
    // =================================================================
    private fun showCategories() {
        onBack = { showList() }
        render = { showCategories() }

        val box = vbox()
        val head = hbox()
        head.addView(label(t("categories"), 20f, true), LinearLayout.LayoutParams(0, WRAP, 1f))
        head.addView(button(t("add_category")) { categoryDialog(null) })
        box.addView(head)

        val cats = safe { db.getCategories() } ?: emptyList()
        if (cats.isEmpty()) box.addView(label(t("no_categories_list")), lp(12))
        cats.forEach { c ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(8), 0, dp(8))
            }
            row.addView(label(c.name, 17f, true))
            if (c.description.isNotBlank()) row.addView(label(c.description, 14f))
            val actions = hbox()
            actions.addView(button(t("edit")) { categoryDialog(c) })
            actions.addView(button(t("delete")) {
                confirm(t("confirm_delete_category")) { deleteCategory(c.id) }
            })
            row.addView(actions)
            box.addView(row)
            box.addView(divider())
        }
        box.addView(button(t("back")) { showList() }, lp(12))
        setScreen(box)
    }

    private fun categoryDialog(existing: DishCategory?) {
        val form = vbox()
        val etName = input(t("category_name"), InputType.TYPE_CLASS_TEXT, existing?.name ?: "")
        val etDesc = input(t("description"), InputType.TYPE_CLASS_TEXT, existing?.description ?: "")
        form.addView(etName)
        form.addView(etDesc)

        val dialog = AlertDialog.Builder(this)
            .setTitle(t(if (existing == null) "add_category" else "edit_category"))
            .setView(form)
            .setPositiveButton(t("save"), null)       // gán listener bên dưới để lỗi thì không đóng hộp thoại
            .setNegativeButton(t("cancel"), null)
            .create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                etName.error = t("err_name_empty")
                return@setOnClickListener
            }
            val cat = DishCategory(existing?.id ?: 0, name, etDesc.text.toString())
            safe { if (existing == null) db.insertCategory(cat) else db.updateCategory(cat) }
                ?: return@setOnClickListener
            dialog.dismiss()
            showCategories()
        }
    }

    private fun deleteCategory(id: Int) {
        try {
            when (db.deleteCategory(id)) {
                DeleteResult.DELETED -> toast(t("deleted"))
                DeleteResult.IN_USE -> toast(t("err_category_in_use"))
                DeleteResult.NOT_FOUND -> toast(t("err_not_found"))
            }
        } catch (e: SQLiteException) {
            toast(t("err_db"))
        }
        showCategories()
    }

    // =================================================================
    // Hàm tiện ích dựng giao diện
    // =================================================================
    private fun setScreen(content: View) {
        val sv = ScrollView(this).apply { isFillViewport = true }
        sv.addView(content, FrameLayout.LayoutParams(MATCH, WRAP))
        container.removeAllViews()
        container.addView(sv, FrameLayout.LayoutParams(MATCH, MATCH))
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun vbox() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12), dp(8), dp(12), dp(16))
    }

    private fun hbox() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun lp(top: Int = 0) =
        LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(top) }

    private fun divider() = View(this).apply {
        setBackgroundColor(0x33888888)
        layoutParams = LinearLayout.LayoutParams(MATCH, dp(1))
    }

    private fun label(s: String, size: Float = 14f, bold: Boolean = false, color: Int? = null) =
        TextView(this).apply {
            text = s
            textSize = size
            if (bold) setTypeface(null, Typeface.BOLD)
            if (color != null) setTextColor(color)
        }

    private fun button(s: String, onClick: () -> Unit) = Button(this).apply {
        text = s
        isAllCaps = false
        setOnClickListener { onClick() }
    }

    private fun input(hint: String, type: Int, value: String = "") = EditText(this).apply {
        this.hint = hint
        inputType = type
        setText(value)
    }

    private fun spinner(items: List<String>, selected: Int) = Spinner(this).apply {
        adapter = ArrayAdapter(
            this@MainActivity, android.R.layout.simple_spinner_dropdown_item, items
        )
        setSelection(selected)
    }

    private fun confirm(msg: String, onYes: () -> Unit) {
        AlertDialog.Builder(this)
            .setMessage(msg)
            .setPositiveButton(t("delete")) { _, _ -> onYes() }
            .setNegativeButton(t("cancel"), null)
            .show()
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    /** Bắt lỗi truy cập CSDL, báo cho người dùng thay vì để app bị crash. */
    private fun <T> safe(block: () -> T): T? = try {
        block()
    } catch (e: SQLiteException) {
        toast(t("err_db"))
        null
    }

    private fun speed(d: Dish) = t(if (d.isQuick) "quick" else "regular")

    private fun money(p: Double) = String.format(Locale.US, "%,.0f", p) + " đ"

    private fun num(v: Double) = if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()

    private fun isHttpUrl(s: String) = s.startsWith("http://") || s.startsWith("https://")
}
