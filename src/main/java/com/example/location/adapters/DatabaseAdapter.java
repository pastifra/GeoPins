package com.example.location.adapters;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.location.MainActivity;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DatabaseAdapter {
    FirebaseFirestore database = null;
    public final String ID = "Id", NAME = "Name", LAST = "Last", BIRTH_DATE = "Birth_Date", PROGRESSIVE_NUMBER = "Progressive_Number"
            , TITLE = "Title", BODY = "Body", DATE = "Date", TIME = "Time",VISIBLE = "Visible", VISIBLE_TRUE = "True", VISIBLE_FALSE = "False", DATABASE_DOC_ID = "Document_Id"
            , GEO_POINT = "Geo_Point", COARSE_COORDINATES = "Coarse_Coordinates", RANK = "Rank", AMOUNT = "Amount", NON_VISIBLE_AMOUNT = "Non_Visible_Amount";
    private final String COMMENT_SECTION = "Comment_Section", PROFILE_SECTION = "Profile_Section", SETUP_SECTION = "Setup_Section", COMMENT_CONTROL = "Comment_Control", PROFILE_CONTROL = "Profile_Control";
    private final double MAX_TITLE_CHAR = 40, MAX_BODY_CHAR = 288, RANK_GAIN = 1000;
    private final boolean DATABASE_LOG_SWITCH = false;

    public DatabaseAdapter(FirebaseFirestore db)
    {
        database = db;
        commentAroundListener = null;
        commentOfProfileListener = null;
        profileListener = null;
    }
    //--------------------------------------------------------------------------------------------------------------------
    public static LinkedList< Map> toListConverter( Map< Integer, Map> commentMap){
        if( commentMap == null){
            if( MainActivity.LOG_SWITCH)
                Log.d( "Database Adapter", "toListConverter : Null commentMap");
            return null;
        }
        LinkedList< Map> buffer = new LinkedList<>();
        for( int i = commentMap.size() - 1; i >= 0; i--){
            buffer.add( (Map< String, Object>) commentMap.get( i));
        }
        return buffer;
    }
    public static LatLng toLatLngConverter( GeoPoint input){
        if( input == null){
            if( MainActivity.LOG_SWITCH)
                Log.d( "Database Adapter", "toLatLngConverter : Null geopoint");
            return null;
        }
        return new LatLng( input.getLatitude(), input.getLongitude());
    }
    //-------------------------------------------------------------------------------------------------------------------
    //viene richaimato quando vengono ricevuti i commenti attorno
    OnCommentAroundYouReceived commentAroundListener;
    public interface OnCommentAroundYouReceived{
        void OnComplete(boolean isQueryEmpty, int numberOfComment, Map<Integer, Map> commentAroundYou);
    }
    //viene richaimato quando vengono ricevuti i commenti di un profilo
    OnCommentOfProfileReceived commentOfProfileListener;
    public interface OnCommentOfProfileReceived{
        void OnComplete(boolean isQueryEmpty, int numberOfComment, Map<Integer, Map> commentOfProfile);
    }
    //viene richaimato quando vengono ricevuti i dati di un profilo
    OnProfileReceived profileListener;
    public interface OnProfileReceived{
        void OnComplete(boolean isQueryEmpty, Map<String, String> profile);
    }

    public void setReceiveDataListener(OnCommentAroundYouReceived listener){
        commentAroundListener = listener;
    }
    public void setReceiveDataListener(OnCommentOfProfileReceived listener){
        commentOfProfileListener = listener;
    }
    public void setReceiveDataListener(OnProfileReceived listener){
        profileListener = listener;
    }

    //--------------------------------------------------------------------------------------------------------------------------------

    //aggiorna automaticamente il numero di commenti o profili
    private void automaticControlUpdate( final String docId, final String field, final boolean add){
        database.collection( SETUP_SECTION).document( docId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if( task.isSuccessful()){
                    Map< String, Object> globalCommentMap = task.getResult().getData();
                    long value = (long) globalCommentMap.get( field);
                    if( add)
                        value++;
                    else
                    if( value > 0)
                        value--;
                    database.collection( SETUP_SECTION).document( docId).update( field, value).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if( DATABASE_LOG_SWITCH)
                                Log.d(SETUP_SECTION, "Updated number of  " + field + " " + docId);
                        }
                    });
                }
            }
        });
    }

    //rimuove definitivamente un documento
    private void wipeDocument( final String section, final String docId){
        database.collection( section).document( docId).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                automaticControlUpdate( AMOUNT, section, false);
                if( DATABASE_LOG_SWITCH)
                    Log.d( section, "wipeDocument : wiped " + docId);
            }
        });
        if(section.equals(PROFILE_SECTION)) //se elimino un profilo elimino anche i commenti
            database.collection( COMMENT_SECTION).whereEqualTo( ID, docId).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if( task.isSuccessful())
                        for (QueryDocumentSnapshot document : task.getResult()){
                            Map< String, Object> updateComVisStruct = document.getData();
                            database.collection( COMMENT_SECTION).document( (String)updateComVisStruct.get(DATABASE_DOC_ID)).delete();
                        }
                }
            });
    }

    //--------------------------------------------------------------------------------------------------------------------------------
    private int getCoarseValueM( double value){
        return (int) (value * 1000);
    }

    private int getCommentRank( String title, String body){
        if( title == null && body == null){
            if( DATABASE_LOG_SWITCH)
                Log.d( COMMENT_SECTION, "getCommentRank : null arguments");
            return 0;
        }else{
            if( title.length() == 0 || body.length() == 0){
                if( DATABASE_LOG_SWITCH)
                    Log.d( COMMENT_SECTION, "getCommentRank : empty arguments");
                return 0;
            }
        }
        double titleRank = 4 * title.length() * ( MAX_TITLE_CHAR - title.length());
        titleRank /= ( MAX_TITLE_CHAR * MAX_TITLE_CHAR);
        if( titleRank <= 0)
            titleRank = 0;
        double bodyRank = 4 * body.length() * ( MAX_BODY_CHAR - body.length());
        bodyRank /= ( MAX_BODY_CHAR * MAX_BODY_CHAR);
        if( bodyRank <= 0)
            bodyRank = 0;
        int rank = (int) (( titleRank + bodyRank) * RANK_GAIN);
        if( DATABASE_LOG_SWITCH)
            Log.d( COMMENT_SECTION, "getCommentRank : rank : " + rank);
        return rank;
    }

    //vero se il punto "toTest" è compreso tra i due punti dati
    private boolean getGeoPointInRange(GeoPoint min, GeoPoint max, GeoPoint toTest){
        return ( min.getLatitude() <= toTest.getLatitude()) && ( max.getLatitude() >= toTest.getLatitude()) && ( min.getLongitude() <= toTest.getLongitude()) && ( max.getLongitude() >= toTest.getLongitude());
    }

    //crea la stringa per la ricerca grossolana
    private String buildCoarseCoordinatesString( int coarseLongitude, int coarseLatitude){
        String buffer = String.valueOf(coarseLatitude);
        if( coarseLatitude < 0){
            buffer = "-" + buffer;
        }else{
            buffer = "+" + buffer;
        }
        buffer = coarseLongitude + buffer;
        if( coarseLongitude < 0){
            buffer = "-" + buffer;
        }else{
            buffer = "+" + buffer;
        }
        if( DATABASE_LOG_SWITCH)
            Log.d(COMMENT_SECTION, "buildCoarseCoordinates : " + buffer);
        return buffer;
    }

    //realizza il vettore di stringhe contenenti tutte le coordinate grossolana da ricercare
    private String[] buildCoarseCoordinatesArray( GeoPoint minCoord, GeoPoint maxCoord, int coordinatesStep){
        int[] coarseCoordinates = new int[4];
        coarseCoordinates[0] = getCoarseValueM(minCoord.getLongitude());    //longitude min
        coarseCoordinates[1] = getCoarseValueM(minCoord.getLatitude());     //latitude min
        coarseCoordinates[2] = getCoarseValueM(maxCoord.getLongitude());    //longitude max
        coarseCoordinates[3] = getCoarseValueM(maxCoord.getLatitude());     //latitude max
        int longitudeStep = coarseCoordinates[2] - coarseCoordinates[0] + 1;
        int latitudeStep = coarseCoordinates[3] - coarseCoordinates[1] + 1;
        if( DATABASE_LOG_SWITCH) {
            Log.d(COMMENT_SECTION, "buildCoarseCoordinatesArray : longitudeStep : " + longitudeStep);
            Log.d(COMMENT_SECTION, "buildCoarseCoordinatesArray : latitudeStep : " + latitudeStep);
            Log.d(COMMENT_SECTION, "buildCoarseCoordinatesArray :  \n" + coarseCoordinates[0] + " " + coarseCoordinates[2] + "\n" + coarseCoordinates[1] + " " + coarseCoordinates[3]);
        }
        String[] bufferArray = new String[ coordinatesStep];
        for( int i = 0; i < longitudeStep; i++){
            for( int j = 0; j < latitudeStep; j++){
                bufferArray[ i * latitudeStep + j ] = buildCoarseCoordinatesString( coarseCoordinates[0] + i, coarseCoordinates[1] + j);
            }
        }
        return bufferArray;
    }

    private String[] arrayAdapter( String[] arrayToAdapt, int index, int length){
        String[] buffer = new String[length];
        System.arraycopy( arrayToAdapt, index, buffer, 0, length);
        return buffer;
    }

    //calcola il numero di "quadrati" da ricercare
    private int getNecessaryStep( GeoPoint minCoord, GeoPoint maxCoord){
        int longitudeStep = getCoarseValueM(maxCoord.getLongitude()) - getCoarseValueM(minCoord.getLongitude()) + 1;
        longitudeStep = Math.abs( longitudeStep);
        int latitudeStep = getCoarseValueM(maxCoord.getLatitude()) - getCoarseValueM(minCoord.getLatitude()) + 1;
        latitudeStep = Math.abs( latitudeStep);
        int step = longitudeStep * latitudeStep;
        if( DATABASE_LOG_SWITCH)
            Log.d(COMMENT_SECTION, "getNecessaryQuery : totalStep : " + step);
        return step;
    }

    //crea la singola query che viene ricercata
    private Query getFormattedQuery( CollectionReference databaseRef, String[] coordinatesArrayString, long progressiveNumber, int maxElements, int index, int length){
        if( DATABASE_LOG_SWITCH)
            Log.d(COMMENT_SECTION, "getFormattedQuery: length : " + length);
        List<String> arrayQuery = Arrays.asList( arrayAdapter( coordinatesArrayString, index, length));
        if( DATABASE_LOG_SWITCH)
            Log.d(COMMENT_SECTION, "getFormattedQuery: arrayQuery: " + arrayQuery);
        if( progressiveNumber < 0)
            progressiveNumber = 0;
        return databaseRef.whereIn(COARSE_COORDINATES, arrayQuery).whereLessThanOrEqualTo( PROGRESSIVE_NUMBER, progressiveNumber).limit( maxElements);
    }

    //crea il vettore di query necessarie
    private Query[] getFormattedQueryArray( CollectionReference databaseRef, String[] queryCoordinatesStringArray, long progressiveNumber, int maxLength, int stepToFind){
        int stepLength = 0;
        int numberOfQuery = ((stepToFind - 1) / 10) + 1; //calcola il numero di query necessarie in base ai "quadrati" da ricercare
        if( DATABASE_LOG_SWITCH)
            Log.d(COMMENT_SECTION, "getFormattedQueryArray : numberOfQuery : " + numberOfQuery);
        maxLength /= numberOfQuery;
        maxLength++;  //evita =0
        if( DATABASE_LOG_SWITCH) {
            Log.d(COMMENT_SECTION, "getFormattedQueryArray : maxLength : " + maxLength);
            Log.d(COMMENT_SECTION, "getFormattedQueryArray : queryCoordinatesStringArray : " + Arrays.toString(queryCoordinatesStringArray));
        }
        Query[] compound = new Query[ numberOfQuery];
        for( int i= 0; i < numberOfQuery; i++){
            if(stepToFind > 10){
                stepToFind -= 10;
                stepLength = 10;
            }else{
                stepLength = stepToFind;
            }
            if( DATABASE_LOG_SWITCH)
                Log.d(COMMENT_SECTION, "getFormattedQueryArray : stepLength : " + stepLength);
            compound[i] = getFormattedQuery( databaseRef, queryCoordinatesStringArray, progressiveNumber, maxLength, i * 10, stepLength);
        }
        return compound;
    }

    /*lancia la query, metodo iterattivo
    * index -> indice della query nel vettore
    * */
    private void readDatabase(final Map<Integer, Map> commentAroundYou, final Query[] queryArray, final GeoPoint minCoord, final GeoPoint maxCoord, final int index, final boolean emptyQuery, final int minRank){
        queryArray[ index-1].get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    if( DATABASE_LOG_SWITCH)
                        Log.d(COMMENT_SECTION, "getCommentAroundYou : query OK : " + (index -1));
                    Map< String, Object> singleComment;
                    boolean isQueryEmpty = emptyQuery;
                    int localIndex = 0;
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        isQueryEmpty = false;
                        singleComment = document.getData();
                        if( VISIBLE_TRUE.equals(singleComment.get(VISIBLE)) && getGeoPointInRange( minCoord, maxCoord, (GeoPoint) singleComment.get(GEO_POINT)) && ((long)singleComment.get(RANK)) >= minRank){
                            if( DATABASE_LOG_SWITCH)
                                Log.d(COMMENT_SECTION, singleComment.toString());
                            commentAroundYou.put( localIndex++, singleComment);
                        }
                    }
                    if(index == 1)
                        commentAroundListener.OnComplete( isQueryEmpty, commentAroundYou.size(), commentAroundYou);
                    else
                        readDatabase( commentAroundYou, queryArray, minCoord, maxCoord, index - 1, isQueryEmpty, minRank);
                }else{
                    if( DATABASE_LOG_SWITCH)
                        Log.d(COMMENT_SECTION, "getCommentAroundYou : query empty : " + (index -1));
                }
            }
        });
    }

    /*metodo di accesso alla ricerca di commenti
    * maxLength -> numero massimo di risultati
    * minRank -> rank minimo da ricercare
    * */
    public void getCommentAroundYou( final GeoPoint minCoord, final GeoPoint maxCoord, final int maxLength, final int minRank){
        database.collection( SETUP_SECTION).document( COMMENT_CONTROL).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) { //per ottenere il massimo Progressive_Number
                if( task.isSuccessful()){
                    DocumentSnapshot document = task.getResult();
                    Map< String, Object> setUpMap = document.getData();
                    long progressiveNumber;
                    if( setUpMap.get( AMOUNT) == null)
                        progressiveNumber = 1;
                    else
                        progressiveNumber = (long) setUpMap.get( AMOUNT);
                    final int necessaryStep = getNecessaryStep( minCoord, maxCoord);
                    final String[] coordinatesStringArray = buildCoarseCoordinatesArray( minCoord, maxCoord, necessaryStep);
                    final Query[] queryArray = getFormattedQueryArray( database.collection(COMMENT_SECTION), coordinatesStringArray, progressiveNumber, maxLength, necessaryStep);
                    final Map< Integer, Map> commentAroundYou = new HashMap<>();
                    final int numberOfQuery = ((necessaryStep -1 )/ 10) + 1;
                    if( DATABASE_LOG_SWITCH)
                        Log.d( COMMENT_SECTION, "getCommentAroundYou : numberOfQuery : " + numberOfQuery);
                    readDatabase( commentAroundYou, queryArray, minCoord, maxCoord, numberOfQuery, false, minRank);
                }
            }
        });
    }

    //ricerca i commenti di un profilo
    public void getCommentFromProfile(String id){
        if( id == null){
            if( DATABASE_LOG_SWITCH)
                Log.d(COMMENT_SECTION, "getProfileFromProfile : Empty ID");
            return;
        }
        database.collection(COMMENT_SECTION).whereEqualTo(ID, id).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    Map<Integer, Map> commentFromProfile = new HashMap<>();
                    Map< String, Object> singleComment;
                    boolean isQueryEmpty = true;
                    int index = 0;
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        isQueryEmpty = false;
                        singleComment = document.getData();
                        commentFromProfile.put(index++, singleComment);
                    }
                    commentOfProfileListener.OnComplete( isQueryEmpty, index, commentFromProfile);
                }
            }
        });
    }

    //-------------------------------------------------------------------------------------------------------------------------------

    /*metodo aggiunta commento
    * id -> codice id del profilo google
    *  */
    public String addComment(String id, String name, GeoPoint geoPoint, String title, String body, String date){
        if(id == null || title == null || body == null  || geoPoint == null || name == null){
            if( DATABASE_LOG_SWITCH)
                Log.d(COMMENT_SECTION, "AddComment : not enough field");
            return null;
        }
        final Map<String, Object> addCommentMap = new HashMap<>();
        int coarseLongitude = getCoarseValueM(geoPoint.getLongitude()), coarseLatitude = getCoarseValueM(geoPoint.getLatitude());
        addCommentMap.put( ID, id);
        addCommentMap.put( NAME, name);
        addCommentMap.put( GEO_POINT, geoPoint);
        addCommentMap.put( RANK, getCommentRank( title, body));
        addCommentMap.put( COARSE_COORDINATES, buildCoarseCoordinatesString( coarseLongitude, coarseLatitude));
        addCommentMap.put( TITLE, title);
        addCommentMap.put( BODY, body);
        addCommentMap.put( DATE, date);
        addCommentMap.put( VISIBLE, VISIBLE_TRUE);
        final DocumentReference documentAdapter = database.collection(COMMENT_SECTION).document(); //viene resitituito l'id del commento generato automaticamente
        String documentAdapterId = documentAdapter.getId();
        addCommentMap.put(DATABASE_DOC_ID, documentAdapterId);
        Log.d(COMMENT_SECTION, "addComment : Id : " + id);
        database.collection(PROFILE_SECTION).whereEqualTo(ID, id).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() { //controllo presenza id nel database
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    for (QueryDocumentSnapshot document : task.getResult()){
                        if( DATABASE_LOG_SWITCH)
                            Log.d(COMMENT_SECTION, "addComment : Id present");
                        setProgressiveCommentNumber( documentAdapter, addCommentMap); //aggiorna il numero progressivo dei commenti
                        break;
                    }
                }else{
                    if( DATABASE_LOG_SWITCH)
                        Log.d(COMMENT_SECTION, "addComment : Id NOT present");
                }
            }
        });
        return documentAdapterId;
    }

    /*documentAdapter -> reference al documento in cui scrivere il commento
    * map -> oggetto contenente i campi del commento
    * */
    private void setProgressiveCommentNumber( final DocumentReference documentAdapter, final Map< String, Object> map){
        database.collection(SETUP_SECTION).document(COMMENT_CONTROL).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() { //aggirona il numero progressivo
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if( task.isSuccessful()){
                    DocumentSnapshot document = task.getResult();
                    long progressiveNumber = (long) document.get(AMOUNT);
                    progressiveNumber++;
                    map.put( PROGRESSIVE_NUMBER, progressiveNumber);
                    if( DATABASE_LOG_SWITCH)
                        Log.d(COMMENT_SECTION, "setProgressiveCommentNumber : progressiveNumber : " + progressiveNumber);
                    addCommentWrite( documentAdapter, map);
                }
            }
        });
    }

    //metodo richiamato da "setProgressiveCommentNumber" per scrivere il commento del database
    private void addCommentWrite( DocumentReference documentAdapter, Map< String, Object> map){
        documentAdapter.set( map).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                automaticControlUpdate( COMMENT_CONTROL, AMOUNT,true);
                if( DATABASE_LOG_SWITCH)
                    Log.d(COMMENT_SECTION, "Written on Database");
            }
        });
    }

    //-----------------------------------------------------------------------------------------------------------------------------------------

    /*metodo per cambiare body o title di un commento o di un profilo
    * docId -> id del documento
    * */
    private void changeField(final String docId, final String section, final String fieldToUpdate, final String valueToUpdate){
         database.collection(section).document(docId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if(documentSnapshot != null) {
                    Map<String, Object> bufferMap = documentSnapshot.getData();
                    if( bufferMap != null){
                        bufferMap.put(fieldToUpdate, valueToUpdate);
                        if(fieldToUpdate.equals(TITLE) || fieldToUpdate.equals(BODY)){
                            int newRank = getCommentRank( (String) bufferMap.get( TITLE), (String) bufferMap.get( BODY));
                            bufferMap.put( RANK, newRank);
                        }
                        changeFieldOnDatabase(docId, section, fieldToUpdate, bufferMap); // funzione cambiamneto campi
                    }else{
                        if( DATABASE_LOG_SWITCH)
                            Log.d(section, "changeField : Null Map reference");
                    }
                }else {
                    if( DATABASE_LOG_SWITCH)
                        Log.d(section, "changeField : Null document Reference");
                }
            }
        });
    }

    //metodo richaiamto da "changeField" per scrivere nel database
    private void changeFieldOnDatabase( String docId, String section, final String fieldToUpdate, final Map<String, Object> bufferMap){
        database.collection(section).document(docId).set(bufferMap).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                if( DATABASE_LOG_SWITCH)
                    Log.d( COMMENT_SECTION, "changed : " + fieldToUpdate + " to : " + bufferMap.get( fieldToUpdate));
            }
        });
    }

    public void updateCommentVisibility( final String docId, final boolean visibility, final boolean nowVisibility){
        if( docId == null) {
            if( DATABASE_LOG_SWITCH)
                Log.d(COMMENT_SECTION, "updateCommentVisibility : Not enough filed");
            return;
        }
        if(visibility){
            changeField(docId, COMMENT_SECTION, VISIBLE, VISIBLE_TRUE);
            if( !nowVisibility)
                automaticControlUpdate( COMMENT_CONTROL, NON_VISIBLE_AMOUNT, false);
        }
        else{
            changeField(docId, COMMENT_SECTION, VISIBLE, VISIBLE_FALSE);
            if( nowVisibility)
                automaticControlUpdate( COMMENT_CONTROL, NON_VISIBLE_AMOUNT, true);
        }
    }

    public void updateProfileVisibility( final String docId, final boolean visibility){
        if( docId == null) {
            if( DATABASE_LOG_SWITCH)
                Log.d(PROFILE_SECTION, "updateProfileVisibility : Not enough filed");
            return;
        }
        if(visibility){
            changeField(docId, PROFILE_SECTION, VISIBLE, VISIBLE_TRUE);
            automaticControlUpdate( PROFILE_CONTROL, NON_VISIBLE_AMOUNT, true);
            database.collection( COMMENT_SECTION).whereEqualTo( ID, docId).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if( task.isSuccessful())
                        for (QueryDocumentSnapshot document : task.getResult()){
                            Map< String, Object> updateComVisStruct; //= new HashMap<>();
                            updateComVisStruct = document.getData();
                            updateCommentVisibility( (String)updateComVisStruct.get(DATABASE_DOC_ID), true, updateComVisStruct.get(VISIBLE).equals(VISIBLE_FALSE));
                        }
                }
            });
        }
        else{
            changeField(docId, PROFILE_SECTION, VISIBLE, VISIBLE_FALSE);
            automaticControlUpdate( PROFILE_CONTROL, NON_VISIBLE_AMOUNT, false);
            database.collection( COMMENT_SECTION).whereEqualTo( ID, docId).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if( task.isSuccessful())
                        for (QueryDocumentSnapshot document : task.getResult()){
                            Map< String, Object> updateComVisStruct = document.getData();
                            updateCommentVisibility( (String)updateComVisStruct.get(DATABASE_DOC_ID), false, updateComVisStruct.get(VISIBLE).equals(VISIBLE_TRUE));
                        }
                }
            });
        }
    }

    public void updateCommentBody( final String docId, final String body){
        if( docId == null || body == null){
            if( DATABASE_LOG_SWITCH)
                Log.d(COMMENT_SECTION, "updateCommentBody : Not enough filed");
            return;
        }
        changeField(docId, COMMENT_SECTION, BODY, body);
    }

    public void updateCommentTitle( final String docId, final String title){
        if( docId == null || title == null){
            if( DATABASE_LOG_SWITCH)
                Log.d(COMMENT_SECTION, "updateCommentBody : Not enough filed");
            return;
        }
        changeField(docId, COMMENT_SECTION, TITLE, title);
    }

    //-----------------------------------------------------------------------------------------------------------------------------------------

    //aggiunge un profilo nel database, "id" è l'id del profilo google
    public void addProfile( final String id, final String name, final String last, final String birthDate){
        if(id == null && name == null && last == null && birthDate == null){
            if( DATABASE_LOG_SWITCH)
                Log.d(PROFILE_SECTION, "add profile : Not enough filed");
            return;
        }
        database.collection(PROFILE_SECTION).whereEqualTo(ID, id).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                boolean idAlreadyPresent = false;
                if(task.isSuccessful()){
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        idAlreadyPresent = true;
                        if( DATABASE_LOG_SWITCH)
                            Log.d(ID, id + " Already Present");
                    }
                    if(!idAlreadyPresent){
                        writeProfileOnDatabase( id, name, last, birthDate); //se non presente scrive nel database
                    }
                }
            }
        });
    }

    private void writeProfileOnDatabase( String id, String name, String last, String birthDate){
        if(id == null || name == null || last == null || birthDate == null)
        {
            if( DATABASE_LOG_SWITCH)
                Log.d(PROFILE_SECTION, "writeProfileOnDatabase : Not enough filed");
            return;
        }
        Map<String, String> addProfileMap = new HashMap<>();
        addProfileMap.put(ID, id);
        addProfileMap.put(NAME, name);
        addProfileMap.put(LAST, last);
        addProfileMap.put(BIRTH_DATE, birthDate);
        addProfileMap.put(VISIBLE, VISIBLE_TRUE);
        database.collection(PROFILE_SECTION).document(id).set(addProfileMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                automaticControlUpdate( PROFILE_CONTROL, AMOUNT,true);
                if( DATABASE_LOG_SWITCH)
                    Log.d(PROFILE_SECTION, "Written on Database");
            }
        });
    }

    //-------------------------------------------------------------------------------------------------------------------------
    //metodo per la ricezione di tutti i dati di un profilo
    public void getProfile(final String id){
        if(id == null){
            if( DATABASE_LOG_SWITCH)
                Log.d(PROFILE_SECTION, "getProfile : Empty ID");
            return;
        }
        database.collection(PROFILE_SECTION).whereEqualTo(ID, id).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                boolean idFound = false;
                Map<String, String> profile = new HashMap<>();
                if( task.isSuccessful()){
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        if ( id.equals(document.getId())) {
                            idFound = true;
                            profile = (Map<String, String>)(Map) document.getData();
                            if(profileListener != null) {
                                profileListener.OnComplete(false, profile);
                            }
                            if( DATABASE_LOG_SWITCH)
                                Log.d(ID, id + " Present");
                        }
                    }
                }
                if(!idFound && profileListener != null){
                    if( DATABASE_LOG_SWITCH)
                        Log.d(ID, id +  "NOT Present");
                    profileListener.OnComplete(true, null);
                }
            }
        });
    }

    public void wipeProfile( final String id){
        if(id == null){
            if( DATABASE_LOG_SWITCH)
                Log.d(PROFILE_SECTION, "wipeProfile : Empty ID");
            return;
        }
        wipeDocument( PROFILE_SECTION, id);
    }

}