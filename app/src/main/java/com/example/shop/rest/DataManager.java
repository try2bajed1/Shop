package com.example.shop.rest;

import android.support.annotation.Nullable;
import com.example.shop.utils.RxUtilsKt;
import com.example.shop.db.CatalogRepo;
import com.example.shop.db.DatabaseHelper;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import java.util.prefs.Preferences;

public class DataManager {

    private final RestRepository restRepository;
    private final DatabaseHelper databaseHelper;
    private final Preferences preferences;
    private final CatalogRepo catalogRepo;

    public DataManager(RestRepository restRepository, DatabaseHelper databaseHelper, Preferences preferences) {
        this.restRepository = restRepository;
        this.databaseHelper = databaseHelper;
        this.preferences = preferences;
        this.catalogRepo = new CatalogRepo(databaseHelper);
    }

    public Observable<CashBoxData> checkPreviousCashbox(String enteredCode) {
        return restRepository.checkPreviousCashbox(enteredCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Object> getToken(String enteredCode) {
        String deviceName = preferences.getUniqueDeviceId();
        String deviceModel = preferences.getPhoneInfo();
        String firebaseToken = preferences.getFirebaseToken();

        return restRepository.getToken(enteredCode, deviceName, deviceModel, firebaseToken)
                .doOnNext(cashBoxData -> saveToken(cashBoxData))
                .flatMap(cashBoxData -> getOthers( ).toObservable())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Completable getOthers( ) {
            return Completable.mergeArrayDelayError(getSellingPointInfo(token)
                            .concatMap(sellingPointInfo -> getOrdersRetry(token, false)).ignoreElements(),
                    getCashier(token).ignoreElements(),
                    syncContractors(token),
                    getGroups(token).ignoreElements(),
                    getContractors(token).ignoreElements(),
                    getCostItems(token).ignoreElements())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Prepared.Set> getOrdersRetry(String token, boolean allAssigned) {
        return Observable.defer(() -> restRepository.getPagedPreparedOrdersByStatus(preferences.getPrefsLastIdPreparedOrder(),
                preferences.getPrefsLastSavedDate_prepared_order(),
                Prepared.getLimit(), token, allAssigned, preferences.getOrderWhiteList()))
                .repeatWhen(observable -> observable) //тут например можно регулировать задержку перед следующим обращением
                .takeUntil((set) -> !set.needMore())
                .doOnNext(orders -> {
                    if (orders.hasNextFromId() && orders.hasNextUpdatedSince()) {
                        orders.createOrUpdate();
                        preferences.saveLastUpdPrepared(orders);
                    }
                })
                .filter((set) -> !set.needMore())
                .doOnComplete(() -> Preferences.getInstance().setLastCompletedOrdersUpdate(System.currentTimeMillis()))
                .onErrorResumeNext(this::processErr);
    }

    //при 401 или 403 пробрасываем ошибку в презентер, чтобы во вью перенаправить на экран ввода кода
    private Observable processErr(Throwable throwable) {
        if (BuildConfig.DEBUG)
            throwable.printStackTrace();
        if (throwable instanceof RetrofitException || throwable instanceof AccessException)
            return Observable.error(throwable);

        return Observable.empty();
    }

    /**
     * Возвращает {@return Observable} для загрузки и сохранения {@link Product} и {@link Variant}
     * Тип возвращаемого значения должен позволять многократный вызов метода onNext для поэтапной загрузки каталога
     *
     * @param withoutArchived - true, если НЕ требуется загружать архивированные товары и категории вместе с неархивированными
     */


    public Observable<Product.Set> getProductsRetry(String token, boolean withoutArchived) {
        return Observable.defer(() -> restRepository.getPagedProducts(preferences.getPrefsLastIdProduct(),
                preferences.getPrefsLastSavedDateProduct(),
                Product.getLimit(), token, withoutArchived))
                .repeatWhen(observable -> observable) //тут например можно регулировать задержку перед следующим обращением
                .takeUntil((set) -> !set.needMore())
                .doOnNext(products -> {
                    if (products.hasNextFromId() && products.hasNextUpdatedSince()) {
                        catalogRepo.saveProducts(products);
                        preferences.saveLastUpdSinceProduct(products);
                    }
                })
                .onErrorResumeNext(this::processErr);
    }

    public Observable<Category.Set> getCategoriesRetry(String token) {
        return Observable.defer(() -> restRepository.getPagedCategories(preferences.getPrefsLastIdCategory(),
                preferences.getPrefsLastSavedDateCategory(), Category.getLimit(), token))
                .repeatWhen(observable -> observable) //тут например можно регулировать задержку перед следующим обращением
                .takeUntil((set) -> !set.needMore())
                .doOnNext(categories -> {
                    if (categories.hasNextFromId() && categories.hasNextUpdatedSince()) {
                        catalogRepo.saveCategories(categories);
                        preferences.saveLastUpdSinceCategory(categories);
                    }
                })
                .onErrorResumeNext(this::processErr);
    }

    public Observable<User.Set> getCashier(String token) {

        return restRepository.getCashier(token)
                .onErrorResumeNext(this::processErr)
                .doOnNext(databaseHelper::saveUsers)
                .doOnComplete(() -> preferences.setLastCompletedCashiersUpdate(System.currentTimeMillis()));
    }

    private Observable<Group.Set> getGroups(String token) {
        return Observable.defer(() -> restRepository.getGroups(preferences.getPrefsLastIdLoyalGroups(),
                preferences.getPrefsLastSavedDateLoyalGroups(), Group.LIMIT, token, true))
                .repeatWhen(observable -> observable)
                .takeUntil((set) -> !set.needMore())
                .doOnNext(groups -> {
                    if (groups.hasNextFromId() && groups.hasNextUpdatedSince()) {
                        databaseHelper.saveGroups(groups);
                        preferences.saveNextUpdatedSinceLoyalGroups(groups);
                    }
                })
                .filter((set) -> !set.needMore())
                .onErrorResumeNext(this::processErr);
    }

    private Completable syncContractors(String token) {
        return RxUtilsKt.setIOScheduler(
                Observable.fromCallable(() -> databaseHelper.getContractorsDao().queryBuilder().where().isNull(Contractor.FILED_ID).query())
                        .flatMap(Observable::fromIterable)
                        .flatMap(contractor -> restRepository.createContractor(contractor, token))
                        .doOnNext(databaseHelper::replaceContractor))
                .ignoreElements();
    }

    private Observable<Contractor.Set> getContractors(String token) {
        return Observable.defer(() -> restRepository.getContractors(preferences.getPrefsLastIdContractor(),
                preferences.getPrefsLastSavedDateContractor(), Contractor.LIMIT, token, true))
                .repeatWhen(observable -> observable)
                .takeUntil((set) -> !set.needMore())
                .doOnNext(contractors -> {
                    if (contractors.hasNextFromId() && contractors.hasNextUpdatedSince()) {
                        databaseHelper.saveContractors(contractors);
                        preferences.saveNextUpdatedSinceContractor(contractors);
                    }
                })
                .filter((set) -> !set.needMore())
                .onErrorResumeNext(this::processErr);
    }

    public Observable<Feedback> getNotSyncFeedback() {
        return Observable.just(databaseHelper.getFeedbackDao().queryForAll())
                .flatMapIterable(feedback -> feedback);
    }

    public Completable sendFeedbackFirstTime(FeedbackServerModel feedbackServerModel, String token) {
        return Single.just(feedbackServerModel)
                .flatMap(o -> restRepository.sendFeedback(o, token))
                .doOnError(throwable -> databaseHelper.saveFeedback(new FeedbackServerModelToFeedbackMapper().map(feedbackServerModel)))
                .toCompletable();
    }

    public Completable sendFeedback(Feedback feedback, String token) {
        return Single.just(feedback)
                .flatMapCompletable(o -> restRepository.sendFeedback(new FeedbackToFeedbackServerModelMapper().map(o), token).toCompletable())
                .doOnComplete(() -> databaseHelper.deleteFeedback(feedback.getUuid()));
    }


    public Observable<SellingPointInfo> getSellingPointInfo(String token) {
        return restRepository.getSellingPointInfo(token)
                .doOnNext(preferences::saveSailingPointInfo)
                .onErrorResumeNext(this::processErr)
                .doOnComplete(() -> Preferences.getInstance().setLastCompletedAccountUpdate(System.currentTimeMillis()));
    }

    private boolean kktReceiptExists(OrderToPost otp) {
        return otp.isRealization()
                || otp.isShipment()
                || otp.isRetailShift();
    }

    private boolean kktReceiptExistsForRefunds(OrderToPost otp) {
        return otp.isRealizationReturn()
                || otp.isShipmentReturn();
    }

    private Observable<Boolean> postUnsynced() {
        Observable<OrderToPost> unsynced = Observable.fromCallable(databaseHelper::queryNotSynced).flatMapIterable(item -> item);
        Observable<KkmCheck> unsyncedZreports = Observable.fromCallable(databaseHelper::getKKMZreports).flatMapIterable(item -> item);

        return unsynced
                .filter(OrderToPost::isPurchase)
                .flatMap(this::postOrderToPost)
                .filter(this::kktReceiptExists) // пока чеки существуют для продаж и возвратов. другие виды вниз по цепочке не пускаем
                .flatMap(this::postKKM, (otp, kkmCheck) -> otp)
                .flatMap(this::postUserCheck, (otp, kkmCheck) -> otp)
                .concatWith(unsynced
                        .filter(orderToPost -> !orderToPost.isPurchase()) // потом возвраты. потому что при отправке возврата надо иметь id продажи
                        .filter(this::kktReceiptExistsForRefunds)
                        .map(orderToPost -> Queries.queryForUuid(orderToPost.getUuid()))
                        .flatMap(this::postOrderToPost)
                        .flatMap(this::postKKM, (otp, kkmCheck) -> otp)
                        .flatMap(this::postUserCheck, (otp, kkmCheck) -> otp))
                .map(orderToPost -> new KkmCheck.FiscalCheck()) //чтобы компилятор не ругался для конката ниже
                .concatWith(unsyncedZreports
                        .flatMap(this::postCloseTurn))
                .map(orderToPost -> true);
    }



    private Observable<OrderToPost> postOrderToPost(OrderToPost orderToPost) {
        return getObsForOrder(orderToPost)
                .onErrorResumeNext(Observable.empty()) // сперва отправляем продажи
                .doOnNext(OrderToPostQueries::createOrUpdate);
    }


    public Completable syncAll(boolean apkSourceMarket, @Nullable String latitude, @Nullable String longitude) {
        //конкат : сначала пачка несинх. отгрузок, затем вся остальная инфа
        return Completable.mergeArrayDelayError(
                getOthers(token, apkSourceMarket, latitude, longitude),
                postUnsynced().ignoreElements()
        )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Observable<OrderToPost> getObsForOrder(OrderToPost orderToPost) {
        String token = token;
        switch (orderToPost.getType()) {
            case TYPE_Realization:
            case TYPE_Shipment:
                return restRepository.postOrder(orderToPost, token);
            case TYPE_Return:
            case TYPE_ShipmentReturn:
                return restRepository.postReturnOrder(orderToPost, token);
            case TYPE_CashInput:
                return restRepository.postCashIncome(orderToPost, token);
            case TYPE_CashOutput:
                return restRepository.postCashWithdrawal(orderToPost, token);
            default:
                return Observable.empty();
        }
    }
}