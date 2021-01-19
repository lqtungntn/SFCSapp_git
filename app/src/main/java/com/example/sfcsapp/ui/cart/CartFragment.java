package com.example.sfcsapp.ui.cart;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.braintreepayments.api.dropin.DropInRequest;
import com.braintreepayments.api.dropin.DropInResult;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.example.sfcsapp.Adapter.MyCartAdapter;
import com.example.sfcsapp.Common.Common;
import com.example.sfcsapp.Common.MySwipeHelper;
import com.example.sfcsapp.Database.CartDataSource;
import com.example.sfcsapp.Database.CartDatabase;
import com.example.sfcsapp.Database.CartItem;
import com.example.sfcsapp.Database.LocalCartDataSource;
import com.example.sfcsapp.EventBus.CounterCartEvent;
import com.example.sfcsapp.EventBus.HideFABCart;
import com.example.sfcsapp.EventBus.UpdateItemInCart;
import com.example.sfcsapp.Model.BraintreeTransaction;
import com.example.sfcsapp.Model.Order;
import com.example.sfcsapp.R;
import com.example.sfcsapp.Remote.ICloudFunctions;
import com.example.sfcsapp.Remote.RetrofitICloudClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.w3c.dom.Text;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class CartFragment extends Fragment {

    private static final int REQUEST_BRAINTREE_CODE = 7777;
    private Parcelable recyclerViewState;
    private CartDataSource cartDataSource;
    private MyCartAdapter adapter;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    LocationRequest locationRequest;
    LocationCallback locationCallback;
    FusedLocationProviderClient fusedLocationProviderClient;
    Location currentLocation;

    String address, comment;

    ICloudFunctions cloudFunctions;

    @BindView(R.id.recycler_cart)
    RecyclerView recycler_cart;
    @BindView(R.id.txt_total_price)
    TextView txt_total_price;
    @BindView(R.id.txt_empty_cart)
    TextView txt_empty_cart;
    @BindView(R.id.group_place_holder)
    CardView group_place_holder;

    @OnClick(R.id.btn_place_order)
    void onPlaceOrderClick(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("One more Step!");
        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_place_order, null);
        EditText edt_address = (EditText)view.findViewById(R.id.edt_address);
        EditText edt_comment = (EditText)view.findViewById(R.id.edt_comment);
        TextView txt_address = (TextView) view.findViewById(R.id.txt_address_detail);
        RadioButton rdi_home = (RadioButton)view.findViewById(R.id.rdi_home_address);
        RadioButton rdi_other_address = (RadioButton)view.findViewById(R.id.rdi_other_address);
        RadioButton rdi_ship_to_this = (RadioButton)view.findViewById(R.id.rdi_ship_this_address);
        RadioButton rdi_cod = (RadioButton)view.findViewById(R.id.rdi_cod);
        RadioButton rdi_braintree = (RadioButton)view.findViewById(R.id.rdi_braintree);

        edt_address.setText(Common.currentUser.getAddress());

        rdi_home.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b){
                edt_address.setText(Common.currentUser.getAddress());
                txt_address.setVisibility(View.GONE);
            }
        });
        rdi_other_address.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b){
                edt_address.setText("");
                edt_address.setHint("Enter Your Address!");
                txt_address.setVisibility(View.GONE);
            }
        });
        rdi_ship_to_this.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b){
                fusedLocationProviderClient.getLastLocation().addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(), ""+e.getMessage(),Toast.LENGTH_SHORT).show();
                        txt_address.setVisibility(View.GONE);
                    }
                }).addOnCompleteListener(task -> {
                    String coordinates = new StringBuilder().append(task.getResult().getLatitude()).append("/")
                            .append(task.getResult().getLongitude()).toString();

                    Single<String> singleAddress = Single.just(getAddressFromLating(task.getResult().getLatitude(), task.getResult().getLongitude()));
                    Disposable disposable = singleAddress.subscribeWith(new DisposableSingleObserver<String>(){
                        @Override
                        public void onSuccess(String s) {
                            edt_address.setText(coordinates);
                            txt_address.setText(s);
                            txt_address.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onError(Throwable e) {
                            edt_address.setText(coordinates);
                            txt_address.setText(e.getMessage());
                            txt_address.setVisibility(View.VISIBLE);
                        }
                    });

                    edt_address.setText(coordinates);
                    txt_address.setText("Implement late");
                    txt_address.setVisibility(View.VISIBLE);
                });
            }
        });

        builder.setView(view);
        builder.setNegativeButton("NO", (dialogInterface, i) -> {
            dialogInterface.dismiss();
        }).setPositiveButton("YES", (dialogInterface, i) -> {
            if (rdi_cod.isChecked()){
                paymentCOD(edt_address.getText().toString(), edt_comment.getText().toString());
            }
            else if(rdi_braintree.isChecked()){
                address = edt_address.getText().toString();
                comment = edt_comment.getText().toString();
                if(!TextUtils.isEmpty(Common.currentToken)){
                    DropInRequest dropInRequest = new DropInRequest().clientToken(Common.currentToken);
                    startActivityForResult(dropInRequest.getIntent(getContext()), REQUEST_BRAINTREE_CODE);
                }
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void paymentCOD(String address, String comment) {
        compositeDisposable.add(cartDataSource.getAllCart(Common.currentUser.getUid()).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Consumer<List<CartItem>>() {
                @Override
                public void accept(List<CartItem> cartItems) throws Exception {
                    cartDataSource.sumPriceInCart(Common.currentUser.getUid()).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new SingleObserver<Double>() {
                                @Override
                                public void onSubscribe(Disposable d) {

                                }

                                @Override
                                public void onSuccess(Double totalPrice) {
                                    Double finalPrice = totalPrice;
                                    Order order = new Order();
                                    order.setUserId(Common.currentUser.getUid());
                                    order.setUserName(Common.currentUser.getName());
                                    order.setUserPhone(Common.currentUser.getPhone());
                                    order.setShippingAddress(address);
                                    order.setComment(comment);
                                    if (currentLocation != null){
                                        order.setLat(currentLocation.getLatitude());
                                        order.setLng(currentLocation.getLongitude());
                                    } else {
                                        order.setLat(-0.1f);
                                        order.setLng(-0.1f);
                                    }
                                    order.setCartItemList(cartItems);
                                    order.setTotalPayment(totalPrice);
                                    order.setDiscount(0);
                                    order.setFinalPayment(finalPrice);
                                    order.setCod(true);
                                    order.setTransactionId("Cash on Delivery");

                                    writeOrderToFirebase(order);
                                }

                                @Override
                                public void onError(Throwable e) {
                                    Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }, throwable -> {
                Toast.makeText(getContext(), ""+throwable.getMessage(), Toast.LENGTH_SHORT).show();
            }));
    }

    private void writeOrderToFirebase(Order order) {
        FirebaseDatabase.getInstance().getReference(Common.ORDER_REF).child(Common.createOrderNumber()).setValue(order)
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                }).addOnCompleteListener(task -> {
                    cartDataSource.cleanCart(Common.currentUser.getUid()).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new SingleObserver<Integer>() {
                                @Override
                                public void onSubscribe(Disposable d) {

                                }

                                @Override
                                public void onSuccess(Integer integer) {
                                    Toast.makeText(getContext(), "Order placed Successfully!",Toast.LENGTH_SHORT).show();
                                    EventBus.getDefault().postSticky(new CounterCartEvent(true));
                                }

                                @Override
                                public void onError(Throwable e) {
                                    Toast.makeText(getContext(), ""+e.getMessage(),Toast.LENGTH_SHORT).show();
                                }
                            });
                });
    }

    private String getAddressFromLating(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        String result = "";
        try {
            List<Address> addressList = geocoder.getFromLocation(latitude, longitude, 1);
            if (addressList != null && addressList.size() > 0) {
                Address address = addressList.get(0);
                StringBuilder sb = new StringBuilder(address.getAddressLine(0));
                result = sb.toString();
            } else {
                result = "Address not found";
            }
        } catch (IOException e){
            e.printStackTrace();
            result = e.getMessage();
        }
        return null;
    }


    private Unbinder unbinder;

    private CartViewModel cartViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        cartViewModel =
                new ViewModelProvider(this).get(CartViewModel.class);
        View root = inflater.inflate(R.layout.fragment_cart, container, false);

        cloudFunctions = RetrofitICloudClient.getInstance().create(ICloudFunctions.class);

        cartViewModel.initCartDataSource(getContext());
        cartViewModel.getMutableLiveDataCartItems().observe(this, new Observer<List<CartItem>>() {
            @Override
            public void onChanged(List<CartItem> cartItems) {
                if (cartItems == null || cartItems.isEmpty()){
                    recycler_cart.setVisibility(View.GONE);
                    group_place_holder.setVisibility(View.GONE);
                    txt_empty_cart.setVisibility(View.VISIBLE);
                } else {
                    recycler_cart.setVisibility(View.VISIBLE);
                    group_place_holder.setVisibility(View.VISIBLE);
                    txt_empty_cart.setVisibility(View.GONE);

                    adapter = new MyCartAdapter(getContext(), cartItems);
                    recycler_cart.setAdapter(adapter);
                }
            }
        });
        unbinder = ButterKnife.bind(this, root);

        initView();
        initLocation();

        return root;
    }

    private void initLocation() {
        buildLocationRequest();
        buildLocationCallBack();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void buildLocationCallBack() {
        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                currentLocation = locationResult.getLastLocation();
            }
        };
    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setSmallestDisplacement(10f);
    }

    private void initView() {

        setHasOptionsMenu(true);

        cartDataSource = new LocalCartDataSource(CartDatabase.getInstance(getContext()).cartDAO());

        EventBus.getDefault().postSticky(new HideFABCart(true));

        recycler_cart.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recycler_cart.setLayoutManager(layoutManager);
        recycler_cart.addItemDecoration(new DividerItemDecoration(getContext(), layoutManager.getOrientation()));

        MySwipeHelper mySwipeHelper = new MySwipeHelper(getContext(), recycler_cart, 200) {
            @Override
            public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buf) {
                buf.add(new MyButton(getContext(), "Delete", 30, 0, Color.parseColor("#FF3c30"),
                        pos -> {
                            CartItem cartItem = adapter.getItemAtPosition(pos);
                            cartDataSource.deleteCartItem(cartItem).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new SingleObserver<Integer>() {
                                        @Override
                                        public void onSubscribe(Disposable d) {

                                        }

                                        @Override
                                        public void onSuccess(Integer integer) {
                                            adapter.notifyItemRemoved(pos);
                                            sumAllItemInCart();
                                            EventBus.getDefault().postSticky(new CounterCartEvent(true));
                                            Toast.makeText(getContext(),"Delete item from cart successful!", Toast.LENGTH_SHORT);
                                        }

                                        @Override
                                        public void onError(Throwable e) {
                                            Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }));
            }
        };

        sumAllItemInCart();
    }

    private void sumAllItemInCart() {
        cartDataSource.sumPriceInCart(Common.currentUser.getUid()).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Double>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(Double price) {
                        txt_total_price.setText(new StringBuilder("Total: $").append(Common.formatPrice(price)));
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (!e.getMessage().contains("Query returned empty"))
                            Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        menu.findItem(R.id.action_settings).setVisible(false);
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.cart_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_clear_cart){
            cartDataSource.cleanCart(Common.currentUser.getUid()).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<Integer>(){
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onSuccess(Integer integer) {
                            Toast.makeText(getContext(), "Clean cart success!", Toast.LENGTH_SHORT).show();
                            EventBus.getDefault().postSticky(new CounterCartEvent(true));
                        }

                        @Override
                        public void onError(Throwable e) {
                            Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {

        if (!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().isRegistered(this);
        }
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().postSticky(new HideFABCart(false));
        cartViewModel.onStop();
        if (EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().unregister(this);
        }
        if (fusedLocationProviderClient != null){
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
        compositeDisposable.clear();

    }

    @Override
    public void onResume() {
        super.onResume();
        if (fusedLocationProviderClient != null)
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onUpdateItemInCartEvent(UpdateItemInCart event){
        if (event.getCartItem() != null){
            recyclerViewState = recycler_cart.getLayoutManager().onSaveInstanceState();
            cartDataSource.updateCartItem(event.getCartItem()).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<Integer>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onSuccess(Integer integer) {
                            calculateTotalPrice();
                            recycler_cart.getLayoutManager().onRestoreInstanceState(recyclerViewState);
                        }

                        @Override
                        public void onError(Throwable e) {
                            Toast.makeText(getContext(), "[UPDATE CART]" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void calculateTotalPrice() {
        cartDataSource.sumPriceInCart(Common.currentUser.getUid()).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Double>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(Double price) {
                        txt_total_price.setText(new StringBuilder("Total: $").append(Common.formatPrice(price)));
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(getContext(), "[SUM CART]" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_BRAINTREE_CODE){
            DropInResult result = data.getParcelableExtra(DropInResult.EXTRA_DROP_IN_RESULT);
            PaymentMethodNonce nonce = result.getPaymentMethodNonce();

            //calculate sum exit
            cartDataSource.sumPriceInCart(Common.currentUser.getUid())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<Double>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onSuccess(Double totalPrice) {

                            //Get all item in cart to create order
                            compositeDisposable.add(cartDataSource.getAllCart(Common.currentUser.getUid())
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(cartItems -> {

                                        //Submit payment
                                        compositeDisposable.add(cloudFunctions.submitPayment(totalPrice,
                                                nonce.getNonce())
                                                .subscribeOn(Schedulers.io())
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribe(braintreeTransaction -> {

                                                    if(braintreeTransaction.isSuccess()){
                                                        Double finalPrice = totalPrice;
                                                        Order order = new Order();
                                                        order.setUserId(Common.currentUser.getUid());
                                                        order.setUserName(Common.currentUser.getName());
                                                        order.setUserPhone(Common.currentUser.getPhone());
                                                        order.setShippingAddress(address);
                                                        order.setComment(comment);
                                                        if (currentLocation != null){
                                                            order.setLat(currentLocation.getLatitude());
                                                            order.setLng(currentLocation.getLongitude());
                                                        } else {
                                                            order.setLat(-0.1f);
                                                            order.setLng(-0.1f);
                                                        }
                                                        order.setCartItemList(cartItems);
                                                        order.setTotalPayment(totalPrice);
                                                        order.setDiscount(0);
                                                        order.setFinalPayment(finalPrice);
                                                        order.setCod(false);
                                                        order.setTransactionId(braintreeTransaction.getTransaction().getId());

                                                        writeOrderToFirebase(order);
                                                    }

                                                }, throwable -> {
                                                    Toast.makeText(getContext(), ""+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                                })
                                        );

                                    }, throwable -> {
                                        Toast.makeText(getContext(), ""+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                    }));
                        }

                        @Override
                        public void onError(Throwable e) {
                            Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}