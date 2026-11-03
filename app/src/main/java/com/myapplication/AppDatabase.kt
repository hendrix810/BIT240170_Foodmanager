package com.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// =====================================================================
// TODO: đổi thành MSSV của bạn (và đổi cả project thành {MSSV}_FoodManager)
// Đây là đoạn cần chỉ rõ trong Video 2: tên Table và tên cột đều có tiền tố MSSV_
// =====================================================================
const val MSSV = "BIT240170"

object DbNames {
    const val DB_NAME = "${MSSV}_FoodManager.db"

    // ---- Bảng ----
    const val T_CATEGORY = "${MSSV}_DishCategory"
    const val T_DISH = "${MSSV}_Dish"
    const val T_IMAGE = "${MSSV}_DishImage"

    // ---- Cột bảng DishCategory ----
    const val C_ID = "${MSSV}_Id"
    const val C_NAME = "${MSSV}_Name"
    const val C_DESC = "${MSSV}_Description"

    // ---- Cột bảng Dish ----
    const val D_ID = "${MSSV}_Id"
    const val D_NAME = "${MSSV}_Name"
    const val D_PRICE = "${MSSV}_Price"
    const val D_TIME = "${MSSV}_PreparationTime"
    const val D_AVAILABLE = "${MSSV}_IsAvailable"
    const val D_DESC = "${MSSV}_Description"
    const val D_CATEGORY_ID = "${MSSV}_DishCategoryId"

    // ---- Cột bảng DishImage ----
    const val I_ID = "${MSSV}_Id"
    const val I_DISH_ID = "${MSSV}_DishId"
    const val I_URL = "${MSSV}_ImageUrl"
    const val I_THUMB = "${MSSV}_IsThumbnail"
}

class AppDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DbNames.DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_VERSION = 1

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: AppDatabase(context).also { instance = it }
            }
    }

    // ------------------------------------------------------------------
    // Tạo CSDL
    // ------------------------------------------------------------------

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)   // bật khóa ngoại (SQLite mặc định tắt)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE ${DbNames.T_CATEGORY} (
                ${DbNames.C_ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${DbNames.C_NAME} TEXT NOT NULL,
                ${DbNames.C_DESC} TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )

        // RESTRICT: không cho xóa loại món đang được món ăn sử dụng
        // UNIQUE(Name, CategoryId): tên món không trùng trong cùng một loại (không phân biệt hoa thường)
        db.execSQL(
            """
            CREATE TABLE ${DbNames.T_DISH} (
                ${DbNames.D_ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${DbNames.D_NAME} TEXT NOT NULL COLLATE NOCASE,
                ${DbNames.D_PRICE} REAL NOT NULL CHECK(${DbNames.D_PRICE} > 0),
                ${DbNames.D_TIME} INTEGER NOT NULL CHECK(${DbNames.D_TIME} > 0),
                ${DbNames.D_AVAILABLE} INTEGER NOT NULL DEFAULT 1,
                ${DbNames.D_DESC} TEXT NOT NULL DEFAULT '',
                ${DbNames.D_CATEGORY_ID} INTEGER NOT NULL,
                FOREIGN KEY(${DbNames.D_CATEGORY_ID})
                    REFERENCES ${DbNames.T_CATEGORY}(${DbNames.C_ID}) ON DELETE RESTRICT,
                UNIQUE(${DbNames.D_NAME}, ${DbNames.D_CATEGORY_ID})
            )
            """.trimIndent()
        )

        // CASCADE: xóa món thì xóa luôn ảnh của món đó
        db.execSQL(
            """
            CREATE TABLE ${DbNames.T_IMAGE} (
                ${DbNames.I_ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${DbNames.I_DISH_ID} INTEGER NOT NULL,
                ${DbNames.I_URL} TEXT NOT NULL,
                ${DbNames.I_THUMB} INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(${DbNames.I_DISH_ID})
                    REFERENCES ${DbNames.T_DISH}(${DbNames.D_ID}) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        seed(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS ${DbNames.T_IMAGE}")
        db.execSQL("DROP TABLE IF EXISTS ${DbNames.T_DISH}")
        db.execSQL("DROP TABLE IF EXISTS ${DbNames.T_CATEGORY}")
        onCreate(db)
    }

    /** Dữ liệu mẫu: 3 loại món, 5 món (đáp ứng "dữ liệu tối thiểu" của đề). */
    private fun seed(db: SQLiteDatabase) {
        fun cat(name: String, desc: String): Long = db.insert(
            DbNames.T_CATEGORY, null,
            ContentValues().apply {
                put(DbNames.C_NAME, name)
                put(DbNames.C_DESC, desc)
            }
        )

        fun dish(
            name: String, price: Double, time: Int, available: Boolean,
            desc: String, catId: Long, imageUrls: List<String>
        ) {
            val dishId = db.insert(
                DbNames.T_DISH, null,
                ContentValues().apply {
                    put(DbNames.D_NAME, name)
                    put(DbNames.D_PRICE, price)
                    put(DbNames.D_TIME, time)
                    put(DbNames.D_AVAILABLE, if (available) 1 else 0)
                    put(DbNames.D_DESC, desc)
                    put(DbNames.D_CATEGORY_ID, catId)
                }
            )
            imageUrls.forEachIndexed { index, url ->
                db.insert(
                    DbNames.T_IMAGE, null,
                    ContentValues().apply {
                        put(DbNames.I_DISH_ID, dishId)
                        put(DbNames.I_URL, url)
                        put(DbNames.I_THUMB, if (index == 0) 1 else 0)   // ảnh đầu tiên là ảnh đại diện
                    }
                )
            }
        }

        fun img(seed: String) = "https://picsum.photos/seed/$seed/400/300"

        val main = cat("Main course", "Rice, noodles and main dishes")
        val starter = cat("Appetizer", "Light dishes served first")
        val drink = cat("Drinks", "Coffee, tea and beverages")

        dish("Beef pho", 55000.0, 20, true, "Traditional Hanoi beef noodle soup", main,
            listOf(img("pho1"), img("pho2")))
        dish("Broken rice", 45000.0, 15, true, "Grilled pork with broken rice", main,
            listOf(img("rice1")))
        dish("Spring rolls", 30000.0, 10, true, "Fried rolls with dipping sauce", starter,
            listOf(img("roll1"), img("roll2"), img("roll3")))
        dish("Egg coffee", 35000.0, 8, true, "Hanoi egg coffee", drink,
            listOf(img("coffee1")))
        dish("Peach tea", 28000.0, 5, false, "Iced peach tea (out of stock)", drink,
            listOf(img("tea1")))
    }

    // ------------------------------------------------------------------
    // DishCategory
    // ------------------------------------------------------------------

    private fun Cursor.toCategory() = DishCategory(
        id = getInt(getColumnIndexOrThrow(DbNames.C_ID)),
        name = getString(getColumnIndexOrThrow(DbNames.C_NAME)),
        description = getString(getColumnIndexOrThrow(DbNames.C_DESC))
    )

    fun getCategories(): List<DishCategory> {
        val list = mutableListOf<DishCategory>()
        readableDatabase.query(
            DbNames.T_CATEGORY, null, null, null, null, null, "${DbNames.C_NAME} COLLATE NOCASE"
        ).use { c -> while (c.moveToNext()) list += c.toCategory() }
        return list
    }

    /** Trả null nếu Id không tồn tại. */
    fun getCategory(id: Int): DishCategory? =
        readableDatabase.query(
            DbNames.T_CATEGORY, null, "${DbNames.C_ID} = ?", arrayOf(id.toString()),
            null, null, null
        ).use { c -> if (c.moveToFirst()) c.toCategory() else null }

    fun insertCategory(category: DishCategory): Long =
        writableDatabase.insert(DbNames.T_CATEGORY, null, ContentValues().apply {
            put(DbNames.C_NAME, category.name.trim())
            put(DbNames.C_DESC, category.description.trim())
        })

    fun updateCategory(category: DishCategory): Boolean =
        writableDatabase.update(
            DbNames.T_CATEGORY,
            ContentValues().apply {
                put(DbNames.C_NAME, category.name.trim())
                put(DbNames.C_DESC, category.description.trim())
            },
            "${DbNames.C_ID} = ?", arrayOf(category.id.toString())
        ) > 0

    fun countDishesInCategory(categoryId: Int): Int =
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM ${DbNames.T_DISH} WHERE ${DbNames.D_CATEGORY_ID} = ?",
            arrayOf(categoryId.toString())
        ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    /** Không cho xóa loại món đang được sử dụng. */
    fun deleteCategory(id: Int): DeleteResult {
        if (getCategory(id) == null) return DeleteResult.NOT_FOUND
        if (countDishesInCategory(id) > 0) return DeleteResult.IN_USE
        val rows = writableDatabase.delete(
            DbNames.T_CATEGORY, "${DbNames.C_ID} = ?", arrayOf(id.toString())
        )
        return if (rows > 0) DeleteResult.DELETED else DeleteResult.NOT_FOUND
    }

    // ------------------------------------------------------------------
    // Dish
    // ------------------------------------------------------------------

    private fun Cursor.toDish(images: List<DishImage>) = Dish(
        id = getInt(getColumnIndexOrThrow(DbNames.D_ID)),
        name = getString(getColumnIndexOrThrow(DbNames.D_NAME)),
        price = getDouble(getColumnIndexOrThrow(DbNames.D_PRICE)),
        preparationTime = getInt(getColumnIndexOrThrow(DbNames.D_TIME)),
        isAvailable = getInt(getColumnIndexOrThrow(DbNames.D_AVAILABLE)) == 1,
        description = getString(getColumnIndexOrThrow(DbNames.D_DESC)),
        dishCategoryId = getInt(getColumnIndexOrThrow(DbNames.D_CATEGORY_ID)),
        images = images
    )

    /**
     * Tìm theo một phần tên, lọc theo loại / đang bán / khoảng giá, sắp xếp.
     * Các điều kiện được kết hợp bằng AND. Món ngừng bán vẫn xuất hiện nếu onlyAvailable = false.
     */
    fun getDishes(filter: DishFilter = DishFilter()): List<Dish> {
        val where = mutableListOf<String>()
        val args = mutableListOf<String>()

        if (filter.keyword.isNotBlank()) {
            where += "${DbNames.D_NAME} LIKE ?"
            args += "%${filter.keyword.trim()}%"
        }
        filter.categoryId?.let {
            where += "${DbNames.D_CATEGORY_ID} = ?"
            args += it.toString()
        }
        if (filter.onlyAvailable) where += "${DbNames.D_AVAILABLE} = 1"
        filter.minPrice?.let {
            where += "${DbNames.D_PRICE} >= CAST(? AS REAL)"
            args += it.toString()
        }
        filter.maxPrice?.let {
            where += "${DbNames.D_PRICE} <= CAST(? AS REAL)"
            args += it.toString()
        }

        val orderBy = when (filter.sort) {
            SortOption.PRICE_ASC -> "${DbNames.D_PRICE} ASC"
            SortOption.PRICE_DESC -> "${DbNames.D_PRICE} DESC"
            SortOption.TIME_ASC -> "${DbNames.D_TIME} ASC"
            SortOption.NONE -> "${DbNames.D_ID} ASC"
        }

        val list = mutableListOf<Dish>()
        readableDatabase.query(
            DbNames.T_DISH, null,
            if (where.isEmpty()) null else where.joinToString(" AND "),
            if (args.isEmpty()) null else args.toTypedArray(),
            null, null, orderBy
        ).use { c ->
            while (c.moveToNext()) {
                val id = c.getInt(c.getColumnIndexOrThrow(DbNames.D_ID))
                list += c.toDish(getImages(id))
            }
        }
        return list
    }

    /** Trả null nếu Id không tồn tại (UI cần xử lý trường hợp này). */
    fun getDish(id: Int): Dish? =
        readableDatabase.query(
            DbNames.T_DISH, null, "${DbNames.D_ID} = ?", arrayOf(id.toString()),
            null, null, null
        ).use { c -> if (c.moveToFirst()) c.toDish(getImages(id)) else null }

    /**
     * Kiểm tra dữ liệu: Name không rỗng; Price > 0; PreparationTime > 0;
     * DishCategoryId hợp lệ; tên không trùng trong cùng loại.
     * Trả về null nếu hợp lệ, ngược lại trả về mã lỗi đầu tiên.
     */
    fun validateDish(dish: Dish): DishError? {
        if (dish.name.isBlank()) return DishError.NAME_EMPTY
        if (dish.price <= 0.0 || dish.price.isNaN()) return DishError.PRICE_INVALID
        if (dish.preparationTime <= 0) return DishError.TIME_INVALID
        if (getCategory(dish.dishCategoryId) == null) return DishError.CATEGORY_INVALID

        // trùng tên trong cùng loại (loại trừ chính món đang sửa)
        val duplicated = readableDatabase.rawQuery(
            """
            SELECT COUNT(*) FROM ${DbNames.T_DISH}
            WHERE ${DbNames.D_NAME} = ? COLLATE NOCASE
              AND ${DbNames.D_CATEGORY_ID} = ?
              AND ${DbNames.D_ID} <> ?
            """.trimIndent(),
            arrayOf(dish.name.trim(), dish.dishCategoryId.toString(), dish.id.toString())
        ).use { c -> c.moveToFirst() && c.getInt(0) > 0 }
        if (duplicated) return DishError.NAME_DUPLICATE

        return null
    }

    private fun Dish.toValues() = ContentValues().apply {
        put(DbNames.D_NAME, name.trim())
        put(DbNames.D_PRICE, price)
        put(DbNames.D_TIME, preparationTime)
        put(DbNames.D_AVAILABLE, if (isAvailable) 1 else 0)
        put(DbNames.D_DESC, description.trim())
        put(DbNames.D_CATEGORY_ID, dishCategoryId)
    }

    /** Gọi validateDish() trước. Trả về Id mới, hoặc -1 nếu lỗi. */
    fun insertDish(dish: Dish): Long = writableDatabase.insert(DbNames.T_DISH, null, dish.toValues())

    fun updateDish(dish: Dish): Boolean =
        writableDatabase.update(
            DbNames.T_DISH, dish.toValues(), "${DbNames.D_ID} = ?", arrayOf(dish.id.toString())
        ) > 0

    /** Ảnh của món bị xóa theo (ON DELETE CASCADE). Hãy hỏi xác nhận ở UI trước khi gọi. */
    fun deleteDish(id: Int): Boolean =
        writableDatabase.delete(DbNames.T_DISH, "${DbNames.D_ID} = ?", arrayOf(id.toString())) > 0

    // ------------------------------------------------------------------
    // DishImage
    // ------------------------------------------------------------------

    private fun Cursor.toImage() = DishImage(
        id = getInt(getColumnIndexOrThrow(DbNames.I_ID)),
        dishId = getInt(getColumnIndexOrThrow(DbNames.I_DISH_ID)),
        imageUrl = getString(getColumnIndexOrThrow(DbNames.I_URL)),
        isThumbnail = getInt(getColumnIndexOrThrow(DbNames.I_THUMB)) == 1
    )

    /** Ảnh đại diện luôn được xếp lên đầu. Danh sách rỗng nếu món chưa có ảnh. */
    fun getImages(dishId: Int): List<DishImage> {
        val list = mutableListOf<DishImage>()
        readableDatabase.query(
            DbNames.T_IMAGE, null, "${DbNames.I_DISH_ID} = ?", arrayOf(dishId.toString()),
            null, null, "${DbNames.I_THUMB} DESC, ${DbNames.I_ID} ASC"
        ).use { c -> while (c.moveToNext()) list += c.toImage() }
        return list
    }

    /**
     * Thêm ảnh bằng ImageUrl. Ảnh đầu tiên của món tự động là ảnh đại diện.
     * Nếu setAsThumbnail = true thì chuyển ảnh đại diện sang ảnh mới (luôn tối đa 1 ảnh đại diện).
     * Trả về Id ảnh mới, hoặc -1 nếu URL rỗng / món không tồn tại.
     */
    fun addImage(dishId: Int, imageUrl: String, setAsThumbnail: Boolean = false): Long {
        val url = imageUrl.trim()
        if (url.isEmpty() || getDish(dishId) == null) return -1

        val db = writableDatabase
        db.beginTransaction()
        try {
            val isFirst = getImages(dishId).isEmpty()
            val makeThumb = isFirst || setAsThumbnail
            if (makeThumb) clearThumbnails(db, dishId)

            val newId = db.insert(DbNames.T_IMAGE, null, ContentValues().apply {
                put(DbNames.I_DISH_ID, dishId)
                put(DbNames.I_URL, url)
                put(DbNames.I_THUMB, if (makeThumb) 1 else 0)
            })
            db.setTransactionSuccessful()
            return newId
        } finally {
            db.endTransaction()
        }
    }

    /** Đổi ảnh đại diện: đặt toàn bộ ảnh khác của món về false rồi bật ảnh được chọn, trong 1 transaction. */
    fun setThumbnail(imageId: Int): Boolean {
        val db = writableDatabase
        val dishId = db.query(
            DbNames.T_IMAGE, arrayOf(DbNames.I_DISH_ID), "${DbNames.I_ID} = ?",
            arrayOf(imageId.toString()), null, null, null
        ).use { c -> if (c.moveToFirst()) c.getInt(0) else return false }

        db.beginTransaction()
        try {
            clearThumbnails(db, dishId)
            db.update(
                DbNames.T_IMAGE,
                ContentValues().apply { put(DbNames.I_THUMB, 1) },
                "${DbNames.I_ID} = ?", arrayOf(imageId.toString())
            )
            db.setTransactionSuccessful()
            return true
        } finally {
            db.endTransaction()
        }
    }

    /** Xóa ảnh. Nếu xóa đúng ảnh đại diện thì ảnh còn lại đầu tiên (nếu có) trở thành ảnh đại diện. */
    fun deleteImage(imageId: Int): Boolean {
        val db = writableDatabase
        val image = db.query(
            DbNames.T_IMAGE, null, "${DbNames.I_ID} = ?", arrayOf(imageId.toString()),
            null, null, null
        ).use { c -> if (c.moveToFirst()) c.toImage() else return false }

        db.beginTransaction()
        try {
            db.delete(DbNames.T_IMAGE, "${DbNames.I_ID} = ?", arrayOf(imageId.toString()))
            if (image.isThumbnail) {
                getImages(image.dishId).firstOrNull()?.let { setThumbnailInternal(db, it.id) }
            }
            db.setTransactionSuccessful()
            return true
        } finally {
            db.endTransaction()
        }
    }

    private fun clearThumbnails(db: SQLiteDatabase, dishId: Int) {
        db.update(
            DbNames.T_IMAGE,
            ContentValues().apply { put(DbNames.I_THUMB, 0) },
            "${DbNames.I_DISH_ID} = ?", arrayOf(dishId.toString())
        )
    }

    private fun setThumbnailInternal(db: SQLiteDatabase, imageId: Int) {
        db.update(
            DbNames.T_IMAGE,
            ContentValues().apply { put(DbNames.I_THUMB, 1) },
            "${DbNames.I_ID} = ?", arrayOf(imageId.toString())
        )
    }
}
