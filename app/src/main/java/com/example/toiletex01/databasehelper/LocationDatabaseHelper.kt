package com.example.toiletex01.databasehelper

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.toiletex01.ToiletEntity


class LocationDatabaseHelper(context: Context) : SQLiteOpenHelper(context, "toiletdb.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        // 이미 DB가 존재한다고 가정. 필요 시 스키마 작성 가능.
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun getAllLocations(): List<ToiletEntity> {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT num, division, toiletName, roadNameAddress, streetNumberAddress, openingHoursDetails, installationDate, latitude, longitude, dataBaseDate, pw FROM toiletdb", null)

        val locations = mutableListOf<ToiletEntity>()
        while (cursor.moveToNext()) {
            val num = cursor.getInt(0)
            val division = cursor.getString(1)
            val toiletName = cursor.getString(2)
            val roadNameAddress = cursor.getString(3)
            val streetNumberAddress = cursor.getString(4)
            val openingHoursDetails = cursor.getString(5)
            val installationDate = cursor.getString(6)
            val latitude = cursor.getString(7)
            val longitude = cursor.getString(8)
            val dataBaseDate = cursor.getString(9)
            val pw = cursor.getString(10)
            locations.add(ToiletEntity(num, division, toiletName, roadNameAddress, streetNumberAddress, openingHoursDetails, installationDate, latitude, longitude, dataBaseDate, pw))
        }
        cursor.close()
        return locations
    }
}