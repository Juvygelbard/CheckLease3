package Fragments;

/**
 * Created by user on 09/01/2016.
 */

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.support.design.widget.FloatingActionButton;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collections;

import adapters.CustomAdapter;
import bgu_apps.checklease.EditApartment;
import bgu_apps.checklease.MainActivity;
import bgu_apps.checklease.R;
import bgu_apps.checklease.ShowApartment;
import data.Apartment;
import data.Data;
import data.Field;
import data.Value;
import db_handle.ApartmentDB;
import android.view.MenuItem;
import android.widget.Toast;

public class ApartmentListFragment extends Fragment {
    private ListView _lv;
    public static ArrayList<Apartment> _apartments;
    public static ArrayList<Apartment> _apartmentsFavs;
    private CustomAdapter _adapter;
    private static int _longClickedApartment;
    private String _pathFiles = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Checklease";
    private boolean _isFav;

    public ApartmentListFragment(boolean isFav){
        _isFav = isFav;
    }

    public void handleIntent(){ // fix- works only on first run... (use service?)
        if(MainActivity._intent == null) {
            MainActivity._intent = this.getActivity().getIntent();
            if (MainActivity._intent != null && MainActivity._intent.ACTION_VIEW.equals(MainActivity._intent.getAction())) {
                Uri ret = MainActivity._intent.getData();
                if (ret != null) {
                    addSentApartment(ret.getPath());
                }
            }
        }
    }

    public void refreshList(){
        if(_isFav) {
            _apartmentsFavs.clear();
            _apartmentsFavs.addAll(ApartmentDB.getInstance().getFavoriteList());
            sort(_apartmentsFavs);
        }
        else {
            _apartments.clear();
            _apartments.addAll(ApartmentDB.getInstance().getApartmentList());
            sort(_apartments);
        }
        if(_adapter != null)
            _adapter.notifyDataSetChanged();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        final View layout = inflater.inflate(R.layout.fragment_apartment_list, container, false);
        _lv = (ListView)layout.findViewById(R.id.ApartmentList);
        if(_isFav) {
            _apartmentsFavs = ApartmentDB.getInstance().getFavoriteList();
            _adapter = new CustomAdapter(_apartmentsFavs, inflater);
            sort(_apartmentsFavs);
        }
        else {
            _apartments = ApartmentDB.getInstance().getApartmentList();
            _adapter = new CustomAdapter(_apartments, inflater);
            sort(_apartments);
        }

        _lv.setAdapter(_adapter);
        this.registerForContextMenu(_lv);

        _lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                _longClickedApartment = position;
                return false;
            }
        });
        _lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent showApartment = new Intent(ApartmentListFragment.this.getActivity(), ShowApartment.class);
                showApartment.putExtra("AppIndex", position);
                ApartmentListFragment.this.startActivity(showApartment);
            }
        });

        FloatingActionButton fab = (FloatingActionButton)layout.findViewById(R.id.add_button);
        if(!_isFav) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent addApartment = new Intent(ApartmentListFragment.this.getActivity(), EditApartment.class);
                    addApartment.putExtra("AppIndex", -1);
                    ApartmentListFragment.this.startActivity(addApartment);
                }
            });
        }
        else
            fab.setVisibility(View.GONE);

        File dir = new File(_pathFiles);
        dir.mkdirs();

        handleIntent();

        return layout;
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = this.getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu, menu);
    }

    public static Apartment getApartmentByIndex(int index){
        if(MainActivity.getCurrTabIndex() == 1)
            return _apartmentsFavs.get(index);
        else
            return _apartments.get(index);
    }

    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.editApartment:
                Intent editApartment = new Intent(ApartmentListFragment.this.getActivity(), EditApartment.class);
                editApartment.putExtra("AppIndex", _longClickedApartment);
                ApartmentListFragment.this.startActivity(editApartment);
                return true;
            case R.id.sendApartment:
                String filePath = _pathFiles + "/Apartment_" + getApartmentByIndex(_longClickedApartment).getId() + ".clt";
                File file = new File(filePath);
                saveFile(file,getApartmentByIndex(_longClickedApartment).getFeatureIterator());
                Uri fileUri = Uri.parse(filePath);
                Intent msgIntent = new Intent();
                msgIntent.setAction(Intent.ACTION_SEND);
                msgIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                msgIntent.setType("*/*");
                this.getActivity().startActivity(msgIntent);
                return true;
            case R.id.callApartment:
                ArrayList<String> options = Data.getPhonefieldList();
                switch(options.size()){
                    case 0: // no phone options in list.
                        Toast msg = Toast.makeText(this.getActivity().getApplicationContext(),"לא קיים מספר טלפון בפרטי הדירה!", Toast.LENGTH_SHORT);
                        msg.show();
                        break;
                    case 1: // only one phone option in list.
                        int id = Data.getPhonefieldID(options.get(0));
                        String phone = getApartmentByIndex(_longClickedApartment).getValue(id).getStrValue();
                        Intent dial = new Intent(Intent.ACTION_DIAL);
                        dial.setData(Uri.parse("tel:" + phone));
                        this.startActivity(dial);
                        break;
                    default: // more than one phone option in list.
                        final String[] phoneList = options.toArray(new String[options.size()]);
                        ListDialogFragment choosePhone = new ListDialogFragment("למי להתקשר?", phoneList, new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                int id = Data.getPhonefieldID(phoneList[which]);
                                String phone = getApartmentByIndex(_longClickedApartment).getValue(id).getStrValue();
                                Intent dial = new Intent(Intent.ACTION_DIAL);
                                dial.setData(Uri.parse("tel:" + phone));
                                ApartmentListFragment.this.startActivity(dial);
                            }
                        });
                        choosePhone.show(getFragmentManager(), "רשימת טלפונים");
                        break;
                }
                return true;
            case R.id.deleteApartment:
                Apartment currApartment = getApartmentByIndex(_longClickedApartment);
                Data.getDeletedApartments().add(currApartment);
                ApartmentDB apartmentDB = ApartmentDB.getInstance();
                apartmentDB.deleteApartment(currApartment.getId());
                if(MainActivity.getCurrTabIndex() == 0) {
                    _apartments.remove(_longClickedApartment);
                    MainActivity._favListFragment.refreshList();
                    MainActivity._mapFragment.refreshMap();
                }
                else {
                    _apartmentsFavs.remove(_longClickedApartment);
                    MainActivity._fullListFragment.refreshList();
                    MainActivity._mapFragment.refreshMap();
                }
                if(_adapter != null)
                    _adapter.notifyDataSetChanged();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void saveFile(File file, Iterator<HashMap.Entry<Integer, Value>> iterator){
        try {
            FileOutputStream fos = new FileOutputStream(file);
            try {
                while (iterator.hasNext()) {
                    HashMap.Entry pair = (HashMap.Entry) iterator.next();
                    Value v = (Value) pair.getValue();
                    String str_val = v.getStrValue();
                    String int_val = "" + v.getIntValue();
                    String key = pair.getKey().toString();
                    fos.write(key.getBytes());
                    fos.write(Data.VAL_SEPARATOR_A.getBytes());
                    fos.write(str_val.getBytes());
                    fos.write(Data.VAL_SEPARATOR_A.getBytes());
                    fos.write(int_val.getBytes());
                    fos.write(Data.VAL_SEPARATOR_B.getBytes());
                }
            } catch (IOException e) {
            } finally {
                try {
                    fos.close();
                } catch (IOException e) { }
            }
        }
        catch (FileNotFoundException e) {}
    }



    //sort the apartments according the preferences.
    public void sort(ArrayList<Apartment> listToSort){
        switch (Data.getSortBy()){
            case (Data.SORT_DEF):
                sortDefault(listToSort);
                break;
            case (Data.SORT_FAV):
                sortAllByFav(listToSort);
                break;
            case (Data.SORT_ID):
                sortAllByDate(listToSort);
                break;
            case (Data.SORT_PRICE_DOWNTOUP):
                sortByPriceLow(listToSort);
                break;
            case (Data.SORT_PRICE_UPTODOWN):
                sortByPriceHigh(listToSort);
                break;
            default:
                sortDefault(listToSort);
                break;
        }
    }
// Sort the apartments in the list according to their profitability.
    public void sortDefault(ArrayList<Apartment> listToSort){
        Collections.sort(listToSort, new Comparator<Apartment>() {
                    @Override
                    public int compare(Apartment lhs, Apartment rhs) {
                        int lCalcPrice = lhs.getCalcPrice();
                        int rCalcPrice = rhs.getCalcPrice();
                        if(lCalcPrice == 0)
                            lCalcPrice = 1;
                        if(rCalcPrice == 0)
                            rCalcPrice = 1;
                        Float l = (float) lhs.getGivenPrice()/ (float) lCalcPrice;
                        Float r = (float) rhs.getGivenPrice()/ (float) rCalcPrice;
                        return l.compareTo(r);
                    }
                }
        );
    }

// Sort the apartments in the list:first by favourites and then according to their profitability.
    public void sortAllByFav(ArrayList<Apartment> listToSort){
        Collections.sort(listToSort, new Comparator<Apartment>() {
            @Override
            public int compare(Apartment lhs, Apartment rhs) {
                if (lhs.isFavorite() && !(rhs.isFavorite()))
                    return -1;
                else if (!(lhs.isFavorite()) && (rhs.isFavorite()))
                    return 1;
                int lCalcPrice = lhs.getCalcPrice();
                int rCalcPrice = rhs.getCalcPrice();
                if(lCalcPrice == 0)
                    lCalcPrice = 1;
                if(rCalcPrice == 0)
                    rCalcPrice = 1;
                Float l = (float) lhs.getGivenPrice() / (float) lCalcPrice;
                Float r = (float) rhs.getGivenPrice() / (float) rCalcPrice;

                return l.compareTo(r);
            }
        });
    }

    //sort the apartments in the list by their ID (the order their were added)
    public void sortAllByDate(ArrayList<Apartment> listToSort){
        Collections.sort(listToSort , new Comparator<Apartment>() {
            @Override
            public int compare(Apartment lhs, Apartment rhs) {
                if(lhs.getId() < rhs.getId())
                    return -1;
                else
                    return 1;
            }
        });
    }

    //sort the apartments from the low price to the high. if the prices are equal sort by profitability.
    public void sortByPriceLow(ArrayList<Apartment> listToSort){
        Collections.sort(listToSort , new Comparator<Apartment>() {
            @Override
            public int compare(Apartment lhs, Apartment rhs) {
                if (lhs.getGivenPrice() > rhs.getGivenPrice())
                    return 1;
                else if (lhs.getGivenPrice() < rhs.getGivenPrice())
                    return -1;
                else{

                    Float l = (float) lhs.getGivenPrice()/ (float) lhs.getCalcPrice();
                    Float r = (float) rhs.getGivenPrice()/ (float) rhs.getCalcPrice();
                    return l.compareTo(r);
                }
            }
        });
    }

    //sort the apartments from the high price to the low. if the prices are equal sort by profitability.
    public void sortByPriceHigh(ArrayList<Apartment> listToSort){
        Collections.sort(listToSort , new Comparator<Apartment>() {
            @Override
            public int compare(Apartment lhs, Apartment rhs) {
                if(lhs.getGivenPrice() < rhs.getGivenPrice())
                    return 1;
                else if (lhs.getGivenPrice() > rhs.getGivenPrice())
                    return -1;
                else{
                    int lCalcPrice = lhs.getCalcPrice();
                    int rCalcPrice = rhs.getCalcPrice();
                    if(lCalcPrice == 0)
                        lCalcPrice = 1;
                    if(rCalcPrice == 0)
                        rCalcPrice = 1;
                    Float l = (float) lhs.getGivenPrice()/ (float) lCalcPrice;
                    Float r = (float) rhs.getGivenPrice()/ (float) rCalcPrice;
                    return l.compareTo(r);
                }
            }
        });
    }

    private void loadFile(File file) {
        try {
            Apartment added = new Apartment(Data.getCurrApartmentCounter());
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String fromFile = br.readLine();
            while (fromFile != null) {
                sb.append(fromFile);
                fromFile = br.readLine();
            }
            fromFile = sb.toString();
            String[] vals = fromFile.split(Data.VAL_SEPARATOR_B);
            for (int i = 0; i < vals.length-1; i++) {
                String[] val = vals[i].split(Data.VAL_SEPARATOR_A);
                int id = Integer.parseInt(val[0]);
                String strVal = val[1];
                int intVal = Integer.parseInt(val[2]);
                added.addValue(id, new Value(strVal, intVal));
            }
            added.setFavorite(false);
            ApartmentDB.getInstance().addApartment(added);
            MainActivity._fullListFragment.refreshList();
            MainActivity._mapFragment.refreshMap();
            Toast msg = Toast.makeText(this.getActivity().getApplicationContext(), "נוספה דירה חדשה!", Toast.LENGTH_SHORT);
            msg.show();
            Data.increaseApartmentCounter();
        } catch (IOException e) { }
        catch (IndexOutOfBoundsException e){
            Toast msg = Toast.makeText(this.getActivity().getApplicationContext(), "קובץ הדירה אינו חוקי!", Toast.LENGTH_SHORT);
            msg.show();
        }
    }

    public void addSentApartment(final String path){
        YesNoDialogFragment askToAdd = new YesNoDialogFragment("האם להוסיף את הדירה לרשימה?",
                new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // to add
                        loadFile(new File(path));
                        dialog.dismiss();
                    }
                },
                new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // not to add- do nothing.
                        dialog.dismiss();
                    }
                });
        askToAdd.show(getFragmentManager(), "הוספת דירה");
    }

    public static void reCalcApartmentList(ArrayList<Field> fields){
        ApartmentDB appDB = ApartmentDB.getInstance();

        for(Apartment apartment: _apartments){
            int id = apartment.getId();
            ArrayList<Value> vals = Field.matchParmasToFields(apartment);
            Apartment updated = new Apartment(id);
            for(int i=0; i<fields.size(); i++){
                updated.addValue(fields.get(i).getId(), vals.get(i));
            }
            updated.addValue(Data.ADDRESS_ID, apartment.getValue(Data.ADDRESS_ID));
            updated.addValue(Data.APARTMENT_NUM, apartment.getValue(Data.APARTMENT_NUM));
            updated.addValue(Data.ADDRESS_LAT, apartment.getValue(Data.ADDRESS_LAT));
            updated.addValue(Data.ADDRESS_LAN, apartment.getValue(Data.ADDRESS_LAN));
            updated.addValue(Data.ADDRESS_STR, apartment.getValue(Data.ADDRESS_STR));
            updated.addValue(Data.CALC_PRICE, apartment.getValue(Data.CALC_PRICE));
            updated.addValue(Data.GIVEN_PRICE, apartment.getValue(Data.GIVEN_PRICE));
            updated.addValue(Data.FAVORITE, apartment.getValue(Data.FAVORITE));

            appDB.deleteApartment(id);
            appDB.addApartment(updated);
        }
    }
}