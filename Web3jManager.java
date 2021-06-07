package web3j;

import com.elvishew.xlog.XLog;

import org.web3j.abi.DefaultFunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.EthSign;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;


/*
 * Created by apple on 2020-10-19
 */
public class Web3jManager {

    private Web3jManager() {

    }

    private static Web3jManager instance;
    public static Web3jManager getInstance() {
        synchronized (Web3jManager.class){
            if (instance == null){
                instance = new Web3jManager();
            }
        }
        return instance;
    }

    private Web3j web3j;

    private void init() {
        if (web3j == null){
            web3j = Web3j.build(new HttpService(Constants.chainUrl()));
        }
    }

    private void resetWeb3j() {
        if (web3j != null){
            web3j.shutdown();
        }
        init();
    }

    public void gas(Observer<BigInteger> observer) {
        Observable<BigInteger> observable = Observable.create(new ObservableOnSubscribe<BigInteger>() {
            @Override
            public void subscribe(ObservableEmitter<BigInteger> emitter) throws Exception {
                resetWeb3j();
                try{
                    EthGasPrice gasPrice = web3j.ethGasPrice().send();
                    emitter.onNext(gasPrice.getGasPrice());
                }catch (Exception e){
                    emitter.onError(e);
                }
                emitter.onComplete();
            }
        });

        observable.subscribeOn(Schedulers.io()) 
                .observeOn(AndroidSchedulers.mainThread()) 
                .subscribe(observer);
    }

    public void gasLimit(Observer<BigInteger> observer) {
        Observable<BigInteger> observable = Observable.create(new ObservableOnSubscribe<BigInteger>() {
            @Override
            public void subscribe(ObservableEmitter<BigInteger> emitter) throws Exception {
                resetWeb3j();
                try{
                    EthEstimateGas gasLimit = web3j.ethEstimateGas(null).send();
                    emitter.onNext(gasLimit.getAmountUsed());
                }catch (Exception e){
                    emitter.onError(e);
                }
                emitter.onComplete();
            }
        });

        observable.subscribeOn(Schedulers.io()) 
                .observeOn(AndroidSchedulers.mainThread()) 
                .subscribe(observer);
    }

    public void totalSupply(String conAddr, Observer<BigInteger> observer) {
        Observable<BigInteger> observable = Observable.create(new ObservableOnSubscribe<BigInteger>() {
            @Override
            public void subscribe(ObservableEmitter<BigInteger> emitter) throws Exception {
                resetWeb3j();
                try{
                    BaseContract contract = new BaseContract("", conAddr, web3j, Crypto.getCredentials(), null);
                    BigInteger balance = contract.totalSupply().send();
                    emitter.onNext(balance);
                }catch (Exception e){
                    emitter.onError(e);
                }
                emitter.onComplete();
            }
        });

        observable.subscribeOn(Schedulers.io()) 
                .observeOn(AndroidSchedulers.mainThread()) 
                .subscribe(observer);
    }

    public void transferErc20(String conAddr, String to, String amount, String gasP, String gasL, String remark, Observer<EthSendTransaction> observer) {

        Observable<EthSendTransaction> observable = Observable.create(new ObservableOnSubscribe<EthSendTransaction>() {
            @Override
            public void subscribe(ObservableEmitter<EthSendTransaction> emitter) throws Exception {
                resetWeb3j();
                try {
                    final Function function = new Function(
                            "transfer",
                            Arrays.<Type>asList(new Address(to),
                                    new Uint256(new BigInteger(amount))),
                            Collections.<TypeReference<?>>emptyList());
                    String data = new DefaultFunctionEncoder().encodeFunction(function);
                    XLog.d("transferErc20 " + data);

                    RawTransactionManager transactionManager = new RawTransactionManager(web3j, Crypto.getCredentials(), Constants.chainId());

                    EthSendTransaction sendTransactionResult = transactionManager.sendTransaction(new BigInteger(gasP), new BigInteger(gasL), conAddr, data, new BigInteger("0"));

                    emitter.onNext(sendTransactionResult);
                } catch (Exception e) {
                    XLog.d("ERC20 TRANSFER EX " + e);
                    emitter.onError(e);
                }
                emitter.onComplete();
            }
        });

        observable.subscribeOn(Schedulers.io()) 
                .observeOn(AndroidSchedulers.mainThread()) 
                .subscribe(observer);

    }

    public void getNewBlockNum(Observer<BigInteger> observer) {
        Observable<BigInteger> observable = Observable.create(new ObservableOnSubscribe<BigInteger>() {
            @Override
            public void subscribe(ObservableEmitter<BigInteger> emitter) throws Exception {
                resetWeb3j();
                try {
                    EthBlockNumber blockNumber = web3j.ethBlockNumber().send();
                    emitter.onNext(blockNumber.getBlockNumber());
                } catch (Exception e) {
                    emitter.onError(e);
                }
                emitter.onComplete();
            }
        });

        observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);

    }

    public void getPair(String token0, String token1, Observer<String> observer) {
        Observable<String> observable = Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                resetWeb3j();

                try {
                    Uniswapfactory contract = new Uniswapfactory("", Constants.dexFactory(), web3j, Crypto.getCredentials(), new CustomGasProvider(new BigInteger("0"), new BigInteger("0")));

                    String receipt = contract.getPair(token0, token1).send();
                    emitter.onNext(receipt);

                } catch (Exception e) {
                    emitter.onError(e);
                }
                emitter.onComplete();

            }
        });

        observable.subscribeOn(Schedulers.io()) 
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);
    }

    public String signString(String to, String amount, String gasP, String gasL, String data) {

        byte[] signedMessage = null;
        try {
            RawTransaction rawTransaction = RawTransaction.createTransaction(getNonce(), new BigInteger(gasP), new BigInteger(gasL), to, new BigInteger(amount), data);
            signedMessage = TransactionEncoder.signMessage(rawTransaction, Constants.chainId(), Crypto.getCredentials());
            return Numeric.toHexString(signedMessage);
        } catch (IOException e) {
            return null;
        }
    }

    public void signString(String data, Observer<EthSign> observer) {
        Observable<EthSign> observable = Observable.create(new ObservableOnSubscribe<EthSign>() {
            @Override
            public void subscribe(ObservableEmitter<EthSign> emitter) throws Exception {
                resetWeb3j();
                try {
                    EthSign ethSign = web3j.ethSign(EthGlobal.getInstance().getAccountAddress(), data).send();
                    XLog.d("ethSign " + ethSign.getSignature());

                    emitter.onNext(ethSign);
                } catch (Exception e) {
                    emitter.onError(e);
                }
                emitter.onComplete();
            }
        });

        observable.subscribeOn(Schedulers.io()) 
                .observeOn(AndroidSchedulers.mainThread()) 
                .subscribe(observer);

    }

    private String ethCall(Transaction transaction) {
        try {
            EthCall call = web3j.ethCall(transaction, DefaultBlockParameterName.fromString("latest")).send();
            return call.getValue();
        } catch (IOException e) {
            return null;
        }
    }

    public void allowance(String address, String conAddr, String spender, Observer<BigInteger> observer) {
        Observable<BigInteger> observable = Observable.create(new ObservableOnSubscribe<BigInteger>() {
            @Override
            public void subscribe(ObservableEmitter<BigInteger> emitter) throws Exception {
                resetWeb3j();
                try{
                    BaseContract contract = new BaseContract("", conAddr, web3j, Crypto.getCredentials(), null);
                    BigInteger balance = contract.allowance(address, spender).send();
                    emitter.onNext(balance);
                }catch (Exception e){
                    emitter.onError(e);
                }
                emitter.onComplete();
            }
        });

        observable.subscribeOn(Schedulers.io()) 
                .observeOn(AndroidSchedulers.mainThread()) 
                .subscribe(observer);
    }

    public void swapExactETHForTokens(String address, String[] tokenConAddrs, String amount, String amountMin, String timeMin, String gasP, String gasL, Observer<EthSendTransaction> observer) {
        Observable<EthSendTransaction> observable = Observable.create(new ObservableOnSubscribe<EthSendTransaction>() {
            @Override
            public void subscribe(ObservableEmitter<EthSendTransaction> emitter) throws Exception {
                resetWeb3j();
                ArrayList<Address> list = new ArrayList<>();
                for (String tokenConAddr : tokenConAddrs) {
                    Address mAddress = new Address(tokenConAddr);
                    list.add(mAddress);
                }

                try {
                    Function function = new Function(
                            "swapExactETHForTokens",
                            Arrays.<Type>asList(new Uint256(new BigInteger(amountMin)),
                                    new org.web3j.abi.datatypes.DynamicArray<Address>(Address.class, list)
                                    ,new Address(address),
                                    new Uint256(new BigInteger(timeMin))
                            ),
                            Collections.<TypeReference<?>>emptyList());

                    String data = new DefaultFunctionEncoder().encodeFunction(function);
                    XLog.d("UniSwap " + data);

                    RawTransactionManager transactionManager = new RawTransactionManager(web3j, Crypto.getCredentials(), Constants.chainId());

                    EthSendTransaction sendTransactionResult = transactionManager.sendTransaction(new BigInteger(gasP), new BigInteger(gasL), Constants.dexRouter(), data, new BigInteger(amount));

                    emitter.onNext(sendTransactionResult);
                } catch (Exception e) {
                    emitter.onError(e);
                }
                emitter.onComplete();

            }
        });

        observable.subscribeOn(Schedulers.io()) 
                .observeOn(AndroidSchedulers.mainThread()) 
                .subscribe(observer);
    }
}
