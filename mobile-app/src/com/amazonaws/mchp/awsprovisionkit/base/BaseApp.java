package com.amazonaws.mchp.awsprovisionkit.base;


import android.app.Application;

public class BaseApp extends Application {

	private static BaseApp baseApp;
	//public static DisplayImageOptions options;

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		baseApp = this;
		//net.nanmu.task.net.ECCrypto.single().toString();
		//initImageLoader(getApplicationContext());

		// ActiveAndroid.initialize(this);

		// JPushInterface.setDebugMode(false);
		// JPushInterface.init(this);
		// JPushInterface.setAliasAndTags(getApplicationContext(),
		// Utility.getDeviceId(this), tags);


		// CrashHandler crashHandler = CrashHandler.getInstance();

		// crashHandler.init(getApplicationContext());

		// 启动应用时，检查有异常产生，并发送
		// startCrashService();
		// mummyhelper
		// PListThreadManager.init();
	}

	public static BaseApp getInstance() {
		return baseApp;
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
	}

	/*
	@SuppressLint("NewApi")
	public static void initImageLoader(Context context) {
		// This configuration tuning is custom. You can tune every option, you
		// may tune some of them,
		// or you can create default configuration by
		// method.
		int memory = (int) Runtime.getRuntime().maxMemory();
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
				// .memoryCacheExtraOptions(480, 800) // default = device screen
				// dimensions
				// .discCacheExtraOptions(480, 800, CompressFormat.JPEG, 75,
				// null)
				// .taskExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
				// .taskExecutorForCachedImages(AsyncTask.THREAD_POOL_EXECUTOR)
				.threadPoolSize(3) // default
				.threadPriority(Thread.NORM_PRIORITY - 1) // default
				.tasksProcessingOrder(QueueProcessingType.FIFO) // default
				.denyCacheImageMultipleSizesInMemory().memoryCache(new LruMemoryCache(memory)).memoryCacheSize(memory)
				.memoryCacheSizePercentage(25).discCacheSize(50 * 1024 * 1024).discCacheFileCount(100)
				.discCacheFileNameGenerator(new HashCodeFileNameGenerator()) // default
				.imageDownloader(new BaseImageDownloader(context)) // default
				.imageDecoder(new BaseImageDecoder(false)) // default
				.defaultDisplayImageOptions(DisplayImageOptions.createSimple()) // default
				.writeDebugLogs() // Remove for release app
				.build();
		// Initialize ImageLoader with configuration.
		ImageLoader.getInstance().init(config);

		options = new DisplayImageOptions.Builder()
				// .showImageOnLoading(R.drawable.ic_stub) // resource or
				// drawable
				.showImageForEmptyUri(R.drawable.default_img) // resource or
																// drawable
				.showImageOnFail(R.drawable.default_img) // resource or drawable
				.resetViewBeforeLoading(false) // default:false
				.delayBeforeLoading(0).cacheInMemory(true) // default:false
				.cacheOnDisc(true) // default:false
				// .preProcessor(null)
				// .postProcessor(null)
				// .extraForDownloader(null)
				.considerExifParams(true) // default
				.imageScaleType(ImageScaleType.EXACTLY_STRETCHED)
																	
				// default
				// .bitmapConfig(Config.ARGB_8888) // default
				// .decodingOptions(null)
				.displayer(new RoundedBitmapDisplayer(720)) // default:SimpleBitmapDisplayer()
				// .handler(new Handler()) // default
				.build();
	}
	*/
}
