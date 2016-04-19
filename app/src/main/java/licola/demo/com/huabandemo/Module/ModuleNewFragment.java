package licola.demo.com.huabandemo.Module;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import java.util.List;

import de.greenrobot.event.EventBus;
import licola.demo.com.huabandemo.API.OnPinsFragmentInteractionListener;
import licola.demo.com.huabandemo.API.OnRefreshFragmentInteractionListener;
import licola.demo.com.huabandemo.Adapter.RecyclerPinsHeadCardAdapter;
import licola.demo.com.huabandemo.Base.BaseRecyclerHeadFragment;
import licola.demo.com.huabandemo.Bean.ListPinsBean;
import licola.demo.com.huabandemo.Bean.PinsAndUserEntity;
import licola.demo.com.huabandemo.HttpUtils.RetrofitService;
import licola.demo.com.huabandemo.Main.MainActivity;
import licola.demo.com.huabandemo.Util.Logger;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by LiCola on  2016/04/18  14:17
 */
public class ModuleNewFragment
        extends BaseRecyclerHeadFragment<RecyclerPinsHeadCardAdapter, List<PinsAndUserEntity>> {
    private static final String TAG = "ModuleNewFragment";

    private int mMaxId = 0;

    //多定义的字段 一共两个 另一个在父类中继承得到
    protected static final String TYPE_TITLE = "TYPE_TITLE";
    protected String mTitle;

    //两个与Activity 交互接口
    private OnPinsFragmentInteractionListener mListener;
    private OnRefreshFragmentInteractionListener mRefreshListener;

    @Override
    protected String getTAG() {
        return this.toString();
    }

    public static ModuleNewFragment newInstance(String type, String title) {
        ModuleNewFragment fragment = new ModuleNewFragment();
        Bundle args = new Bundle();
        args.putString(TYPE_KEY, type);
        args.putString(TYPE_TITLE, title);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void getBundleData(Bundle args) {
        if (args != null) {
            mKey = args.getString(TYPE_KEY);
            mTitle = args.getString(TYPE_TITLE);
        }
    }

    @Override
    protected Subscription getHttpFirst() {
        return RetrofitService.createAvatarService()
                .httpsTypeLimitRx(mAuthorization, mKey, mLimit)
                .map(new Func1<ListPinsBean, List<PinsAndUserEntity>>() {
                    @Override
                    public List<PinsAndUserEntity> call(ListPinsBean listPinsBean) {
                        return listPinsBean.getPins();
                    }
                })
                .subscribeOn(Schedulers.io())//发布者的运行线程 联网操作属于IO操作
                .observeOn(AndroidSchedulers.mainThread())//订阅者的运行线程 在main线程中才能修改UI
                .subscribe(new Subscriber<List<PinsAndUserEntity>>() {
                    @Override
                    public void onCompleted() {
                        mRefreshListener.OnRefreshState(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Logger.d(e.toString());
                        mRefreshListener.OnRefreshState(false);
                        checkException(e);
                    }

                    @Override
                    public void onNext(List<PinsAndUserEntity> result) {
                        Logger.d();
                        //保存maxId值 后续加载需要
                        mMaxId = getMaxId(result);
                        mAdapter.setListNotify(result);
                    }
                });


    }


    /**
     * 从返回联网结果中保存max值 用于下次联网的关键
     *
     * @param result
     * @return
     */
    private int getMaxId(List<PinsAndUserEntity> result) {
        return result.get(result.size() - 1).getPin_id();
    }

    @Override
    protected Subscription getHttpScroll() {
        return RetrofitService.createAvatarService()
                .httpsTypeMaxLimitRx(mAuthorization, mKey, mMaxId, mLimit)
                .map(new Func1<ListPinsBean, List<PinsAndUserEntity>>() {
                    @Override
                    public List<PinsAndUserEntity> call(ListPinsBean listPinsBean) {
                        //取出list对象
                        return listPinsBean.getPins();
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<PinsAndUserEntity>>() {
                    @Override
                    public void onCompleted() {
                        Logger.d();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Logger.d();
                        checkException(e);//检查错误 弹出提示
                    }

                    @Override
                    public void onNext(List<PinsAndUserEntity> pinsEntities) {
                        Logger.d();
                        mMaxId = getMaxId(pinsEntities);
                        mAdapter.addListNotify(pinsEntities);
                    }
                });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if ((context instanceof OnRefreshFragmentInteractionListener)
                && (context instanceof OnPinsFragmentInteractionListener)) {
            mListener = (OnPinsFragmentInteractionListener) context;
            mRefreshListener = (OnRefreshFragmentInteractionListener) context;
        } else {
            throwRuntimeException(context);
        }

        if (context instanceof MainActivity) {
            mAuthorization = ((MainActivity) context).mAuthorization;
        } else if (context instanceof ModuleActivity) {
            mAuthorization = ((ModuleActivity) context).mAuthorization;
        }
    }

    @Override
    protected void initListener() {

        mAdapter.setOnClickItemListener(new RecyclerPinsHeadCardAdapter.OnAdapterListener() {
            @Override
            public void onClickImage(PinsAndUserEntity bean, View view) {
                Logger.d();
                EventBus.getDefault().postSticky(bean);
                mListener.onClickPinsItemImage(bean, view);
            }

            @Override
            public void onClickTitleInfo(PinsAndUserEntity bean, View view) {
                Logger.d();
                EventBus.getDefault().postSticky(bean);
                mListener.onClickPinsItemText(bean, view);
            }

            @Override
            public void onClickInfoGather(PinsAndUserEntity bean, View view) {
                Logger.d();
            }

            @Override
            public void onClickInfoLike(PinsAndUserEntity bean, View view) {
                Logger.d(bean.toString());
//                int count=bean.getLike_count()+1;
//                bean.setLike_count(count);
//                mAdapter.notifyDataSetChanged();
//                startLike();
            }

        });
    }

    @Override
    protected View getHeadView() {
        return null;
    }

    @Override
    protected int getAdapterPosition() {
        return mAdapter.getAdapterPosition();
    }

    @Override
    protected RecyclerPinsHeadCardAdapter setAdapter() {
        return new RecyclerPinsHeadCardAdapter(mRecyclerView);
    }

}
