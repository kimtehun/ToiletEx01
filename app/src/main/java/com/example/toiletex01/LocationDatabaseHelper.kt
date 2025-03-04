package com.example.toiletex01

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log


class LocationDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "toiletdb.db"
        private const val DATABASE_VERSION = 24

        // 테이블 이름과 컬럼명 (실제 DB 스키마에 맞게 수정)
        private const val TABLE_TOILETS = "ex01"
        private const val COLUMN_NUM = "num"
        private const val COLUMN_TOILET_NAME = "toiletName"
        private const val COLUMN_LATITUDE = "latitude"
        private const val COLUMN_LONGITUDE = "longitude"
        private const val COLUMN_PW = "pw"

    }

    override fun onCreate(db: SQLiteDatabase) {
        // 이미 DB가 존재한다고 가정. 필요 시 스키마 작성 가능.
        // ToiletEntity 테이블을 생성하는 쿼리문 (테이블이 없을 경우에만 생성)
//        val createTableQuery = """
//            CREATE TABLE IF NOT EXISTS ex01 (
//                num INTEGER ,
//                toiletName TEXT,
//                latitude TEXT,
//                longitude TEXT,
//                pw TEXT
//            );
//        """.trimIndent()
//
//        db.execSQL(createTableQuery)

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun getAllLocations(): List<SimpleToiletEntity> {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT num, toiletName, latitude, longitude, pw FROM ex01", null)

        val locations = mutableListOf<SimpleToiletEntity>()
        while (cursor.moveToNext()) {
            val num = cursor.getInt(0)
            val toiletName = cursor.getString(1)
            val latitude = cursor.getString(2)
            val longitude = cursor.getString(3)
            val pw = cursor.getString(4)
//            val num = cursor.getInt(0)
//            val division = cursor.getString(1)
//            val toiletName = cursor.getString(2)
//            val roadNameAddress = cursor.getString(3)
//            val streetNumberAddress = cursor.getString(4)
//            val openingHoursDetails = cursor.getString(5)
//            val installationDate = cursor.getString(6)
//            val latitude = cursor.getString(7)
//            val longitude = cursor.getString(8)
//            val dataBaseDate = cursor.getString(9)
//            val pw = cursor.getString(10)
            locations.add(SimpleToiletEntity(num, toiletName,latitude, longitude, pw))
        }
        cursor.close()
        return locations
    }

    fun getToiletById(num: Int): SimpleToiletEntity? {
        val db = this.readableDatabase
        var toilet: SimpleToiletEntity? = null

        Log.d("DB_QUERY", "getToiletById 호출, num: $num")

        // id에 해당하는 행을 조회 (모든 컬럼 선택)
        val cursor = db.query(
            "ex01",
            null, // 모든 컬럼 선택
            "$COLUMN_NUM = ?",
            arrayOf(num.toString()),
            null,
            null,
            null
        )

        if (cursor != null) {
            Log.d("DB_QUERY", "cursor count: ${cursor.count}")
            if (cursor.moveToFirst()) {
                // 각 컬럼의 인덱스를 가져와 값 읽기
                val toiletNum = cursor.getInt(cursor.getColumnIndexOrThrow("num"))
                val toiletName = cursor.getString(cursor.getColumnIndexOrThrow("toiletName"))
                val latitude = cursor.getString(cursor.getColumnIndexOrThrow("latitude"))
                val longitude = cursor.getString(cursor.getColumnIndexOrThrow("longitude"))
                val pw = cursor.getString(cursor.getColumnIndexOrThrow("pw"))

                Log.d("DB_QUERY", "레코드 읽음: toiletNum=$toiletNum, toiletName=$toiletName")

                toilet = SimpleToiletEntity(
                    num = toiletNum,
                    toiletName = toiletName,
                    latitude = latitude,
                    longitude = longitude,
                    pw = pw
                )
            } else {
                Log.e("DB_QUERY", "레코드가 없습니다. num: $num")
            }

            cursor.close()
        } else {
            Log.e("DB_QUERY", "Cursor가 null입니다.")
        }
        return toilet
    }
}