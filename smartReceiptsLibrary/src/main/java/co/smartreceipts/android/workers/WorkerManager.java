package co.smartreceipts.android.workers;

import co.smartreceipts.android.SmartReceiptsApplication;
import android.content.Context;

public class WorkerManager {

	private SmartReceiptsApplication mApplication;
	private AdManager mAdManager;
	
	public WorkerManager(SmartReceiptsApplication application) {
		mApplication = application;
	}
	
	public void onDestroy() {
		mApplication = null;
		mAdManager = null;
	}
	
	public final AdManager getAdManager() {
		if (mAdManager == null) {
			mAdManager = instantiateAdManager();
		}
		return mAdManager;
	}
	
	public final SmartReceiptsApplication getApplication() {
		return mApplication;
	}
	
	/**
	 * Protected method to enable subclasses to create custom instances
	 * @return a AdManager Instance
	 */
	protected AdManager instantiateAdManager() {
		return new AdManager(this);
	}
	
}
