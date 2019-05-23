package com.example.shop.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.example.shop.R;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;


/**
 * Database helper class used to manage the creation and upgrading of your database. This class also usually provides
 * the DAOs used by the other classes.
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

    private static final String DATABASE_NAME = "com.example.shop.db.com.example.shop.db";

    private RuntimeExceptionDao<?, Integer> categoriesDao = null;
    private RuntimeExceptionDao<?, Integer> productsDao = null;
    private RuntimeExceptionDao<?, Integer> productImagesDao = null;
    private RuntimeExceptionDao<?, Integer> variantsDao = null;
    private RuntimeExceptionDao<?, Integer> ordersDao = null;
    private RuntimeExceptionDao<?, Integer> usersDao = null;
    private RuntimeExceptionDao<?, Integer> turnDao = null;
    private RuntimeExceptionDao<?, Integer> paymentDao;
    private RuntimeExceptionDao<?, Integer> costItemDao;
    private RuntimeExceptionDao<?, Integer> checkToMailDao;
    private RuntimeExceptionDao<?, Integer> uploadProductDao;
    private RuntimeExceptionDao<?, Integer> kkmRegistrationDao;
    private RuntimeExceptionDao<?, Integer> userCheckRequestDao;
    private RuntimeExceptionDao<?, Integer> delayedOrdersDao;
    private RuntimeExceptionDao<?, Integer> delayedLinesDao;
    private RuntimeExceptionDao<?, Integer> devicesDao;
    private RuntimeExceptionDao<?, Integer> contractorsDao;
    private RuntimeExceptionDao<?, Integer> loyalityGroupsDao;
    private RuntimeExceptionDao<?, Integer> feedbackDao;

    private RuntimeExceptionDao<?, Integer> receiptsDao;
    private RuntimeExceptionDao<?, Integer> linesDao;
    private RuntimeExceptionDao<?, Integer> resultsDao;
    private RuntimeExceptionDao<?, Integer> errorsDao;
    private RuntimeExceptionDao<?, Integer> operationsDao;


    public DatabaseHelper(Context context, int currAppVersionNum) {
        super(context, DATABASE_NAME, null, currAppVersionNum, R.raw.ormlite_config);
    }

    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
        try {
            createAllTables(connectionSource);
            //руками выставляем индексы, чтобы шустро работал поиск при большом кол-ве записей в базе
            db.execSQL("CREATE INDEX index_realizationline_orderToPost_id_position ON realizationline (orderToPost_id, position); ");
            db.execSQL("CREATE INDEX index_product_category_id_and_position ON product ( category_id, position ) WHERE archived_at IS NULL ;");
            db.execSQL("CREATE INDEX index_variant_product_id ON variant (product_id) ;");
            db.execSQL("CREATE INDEX index_payment_orderToPost_id ON payment (orderToPost_id) ;");
            db.execSQL("CREATE INDEX preparedrealizationline_preparedOrder_id_position ON preparedrealizationline (preparedOrder_id, position) ;");
            db.execSQL("CREATE INDEX index_contractor_title ON Contractors (title) ;");
            db.execSQL("CREATE INDEX index_contractot_discout_card_number ON Contractors (discountCardNumber) ;");
            db.execSQL("CREATE UNIQUE INDEX index_Contractors_uuid ON Contractors (uuid) ;");
            db.execSQL("CREATE INDEX index_ordertopost_activated_at  ON ordertopost ( activated_at ) WHERE archived_at IS NULL ;");

        } catch (final Throwable x) {
            x.printStackTrace();
        }
    }

    /**
     * This is called when your application is upgraded and it has a higher version number. This allows you to adjust
     * the various data to match the new version number.
     */
    public void clearAllTablesExceptDevices() throws SQLException {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        try {

            clearAllTablesExceptDevices();

        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "Can't drop databases", e);
            throw new RuntimeException(e);
        }
    }


    public void clearDB() {
        // из доков: In some configurations, it may be faster to drop and re-create the table.
        // http://ormlite.com/javadoc/ormlite-core/com/j256/ormlite/table/TableUtils.html#clearTable(com.j256.ormlite.support.ConnectionSource,%20java.lang.Class)
        ConnectionSource connectionSource = getConnectionSource();
        try {
            dropAllTables(connectionSource);
            createAllTables(connectionSource);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createAllTables(ConnectionSource connectionSource) throws SQLException {


    }


    private void dropAllTables(ConnectionSource connectionSource) throws SQLException {
        /*drop*/
    }


    public void dropHistoryTable() {
        //dropHistoryTable
    }






    /**
     * Close the database connections and clearByUser any cached DAOs.
     */
    @Override
    public void close() { // todo: add new tables here
        super.close();

        usersDao = null;
        turnDao = null;
        productsDao = null;
        productImagesDao = null;
        categoriesDao = null;
        variantsDao = null;
        ordersDao = null;
        paymentDao = null;
        costItemDao = null;
        checkToMailDao = null;
        uploadProductDao = null;
        kkmRegistrationDao = null;
        delayedLinesDao = null;
        delayedOrdersDao = null;
        devicesDao = null;
        receiptsDao = null;
    }


}
