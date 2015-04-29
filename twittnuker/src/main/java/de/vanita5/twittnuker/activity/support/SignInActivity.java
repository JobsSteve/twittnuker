/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2015 vanita5 <mail@vanita5.de>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2015 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.vanita5.twittnuker.activity.support;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.activity.SettingsActivity;
import de.vanita5.twittnuker.app.TwittnukerApplication;
import de.vanita5.twittnuker.fragment.support.BaseSupportDialogFragment;
import de.vanita5.twittnuker.fragment.support.SupportProgressDialogFragment;
import de.vanita5.twittnuker.provider.TwidereDataStore.Accounts;
import de.vanita5.twittnuker.util.AsyncTaskUtils;
import de.vanita5.twittnuker.util.ContentValuesCreator;
import de.vanita5.twittnuker.util.OAuthPasswordAuthenticator;
import de.vanita5.twittnuker.util.OAuthPasswordAuthenticator.AuthenticationException;
import de.vanita5.twittnuker.util.OAuthPasswordAuthenticator.AuthenticityTokenException;
import de.vanita5.twittnuker.util.OAuthPasswordAuthenticator.WrongUserPassException;
import de.vanita5.twittnuker.util.ParseUtils;
import de.vanita5.twittnuker.util.ThemeUtils;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.util.net.OkHttpClientFactory;
import de.vanita5.twittnuker.util.net.TwidereHostResolverFactory;

import twitter4j.Twitter;
import twitter4j.TwitterConstants;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.BasicAuthorization;
import twitter4j.auth.RequestToken;
import twitter4j.auth.TwipOModeAuthorization;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

import static android.text.TextUtils.isEmpty;
import static de.vanita5.twittnuker.util.ContentValuesCreator.createAccount;
import static de.vanita5.twittnuker.util.Utils.getActivatedAccountIds;
import static de.vanita5.twittnuker.util.Utils.getNonEmptyString;
import static de.vanita5.twittnuker.util.Utils.isUserLoggedIn;
import static de.vanita5.twittnuker.util.Utils.showErrorMessage;
import static de.vanita5.twittnuker.util.Utils.trim;

public class SignInActivity extends BaseDialogWhenLargeActivity implements TwitterConstants, OnClickListener,
        TextWatcher {

	private static final String TWITTER_SIGNUP_URL = "https://twitter.com/signup";
	private static final String EXTRA_API_LAST_CHANGE = "api_last_change";
    public static final String FRAGMENT_TAG_SIGN_IN_PROGRESS = "sign_in_progress";
    private static final String DEFAULT_TWITTER_API_URL_FORMAT = "https://[DOMAIN.]twitter.com/";

	private String mAPIUrlFormat;
	private int mAuthType;
	private String mConsumerKey, mConsumerSecret;
	private String mUsername, mPassword;
	private long mAPIChangeTimestamp;
    private boolean mSameOAuthSigningUrl, mNoVersionSuffix;

	private EditText mEditUsername, mEditPassword;
	private Button mSignInButton, mSignUpButton;
    private LinearLayout mSignInSignUpContainer, mUsernamePasswordContainer;

	private final Handler mHandler = new Handler();
	private TwittnukerApplication mApplication;
	private SharedPreferences mPreferences;
	private ContentResolver mResolver;
	private AbstractSignInTask mTask;

	@Override
	public void afterTextChanged(final Editable s) {

	}

	@Override
    public  int getThemeResourceId() {
        return ThemeUtils.getThemeResource(this);
    }
    @Override
	public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {

	}

	@Override
	public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		switch (requestCode) {
			case REQUEST_EDIT_API: {
				if (resultCode == RESULT_OK) {
					mAPIUrlFormat = data.getStringExtra(Accounts.API_URL_FORMAT);
					mAuthType = data.getIntExtra(Accounts.AUTH_TYPE, Accounts.AUTH_TYPE_OAUTH);
					mSameOAuthSigningUrl = data.getBooleanExtra(Accounts.SAME_OAUTH_SIGNING_URL, false);
                    mNoVersionSuffix = data.getBooleanExtra(Accounts.NO_VERSION_SUFFIX, false);
					mConsumerKey = data.getStringExtra(Accounts.CONSUMER_KEY);
					mConsumerSecret = data.getStringExtra(Accounts.CONSUMER_SECRET);
					final boolean isTwipOMode = mAuthType == Accounts.AUTH_TYPE_TWIP_O_MODE;
					mUsernamePasswordContainer.setVisibility(isTwipOMode ? View.GONE : View.VISIBLE);
                    mSignInSignUpContainer.setOrientation(isTwipOMode ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
				}
				setSignInButton();
				invalidateOptionsMenu();
				break;
			}
			case REQUEST_BROWSER_SIGN_IN: {
				if (resultCode == BaseAppCompatActivity.RESULT_OK && data != null) {
					doLogin(data);
				}
				break;
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onClick(final View v) {
		switch (v.getId()) {
			case R.id.sign_up: {
				final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(TWITTER_SIGNUP_URL));
				startActivity(intent);
				break;
			}
			case R.id.sign_in: {
				doLogin();
				break;
			}
			case R.id.sign_in_method_introduction: {
                final FragmentManager fm = getSupportFragmentManager();
                new SignInMethodIntroductionDialogFragment().show(fm.beginTransaction(),
						"sign_in_method_introduction");
				break;
			}
		}
	}



	@Override
    public void onContentChanged() {
        super.onContentChanged();
		mEditUsername = (EditText) findViewById(R.id.username);
		mEditPassword = (EditText) findViewById(R.id.password);
		mSignInButton = (Button) findViewById(R.id.sign_in);
		mSignUpButton = (Button) findViewById(R.id.sign_up);
        mSignInSignUpContainer = (LinearLayout) findViewById(R.id.sign_in_sign_up);
		mUsernamePasswordContainer = (LinearLayout) findViewById(R.id.username_password);
	}

	@Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sign_in, menu);
        return true;
	}

	@Override
	public void onDestroy() {
		getLoaderManager().destroyLoader(0);
		super.onDestroy();
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case MENU_HOME: {
				final long[] account_ids = getActivatedAccountIds(this);
				if (account_ids.length > 0) {
					onBackPressed();
				}
				break;
			}
			case MENU_SETTINGS: {
                if (mTask != null && mTask.getStatus() == AsyncTask.Status.RUNNING)
                    return false;
				final Intent intent = new Intent(this, SettingsActivity.class);
				startActivity(intent);
				break;
			}
			case MENU_EDIT_API: {
                if (mTask != null && mTask.getStatus() == AsyncTask.Status.RUNNING)
                    return false;
				setDefaultAPI();
				final Intent intent = new Intent(this, APIEditorActivity.class);
				intent.putExtra(Accounts.API_URL_FORMAT, mAPIUrlFormat);
				intent.putExtra(Accounts.AUTH_TYPE, mAuthType);
				intent.putExtra(Accounts.SAME_OAUTH_SIGNING_URL, mSameOAuthSigningUrl);
                intent.putExtra(Accounts.NO_VERSION_SUFFIX, mNoVersionSuffix);
				intent.putExtra(Accounts.CONSUMER_KEY, mConsumerKey);
				intent.putExtra(Accounts.CONSUMER_SECRET, mConsumerSecret);
				startActivityForResult(intent, REQUEST_EDIT_API);
				break;
			}
			case MENU_OPEN_IN_BROWSER: {
				if (mAuthType != Accounts.AUTH_TYPE_OAUTH || mTask != null
                        && mTask.getStatus() == AsyncTask.Status.RUNNING) return false;
				saveEditedText();
				final Intent intent = new Intent(this, BrowserSignInActivity.class);
				intent.putExtra(Accounts.CONSUMER_KEY, mConsumerKey);
				intent.putExtra(Accounts.CONSUMER_SECRET, mConsumerSecret);
				startActivityForResult(intent, REQUEST_BROWSER_SIGN_IN);
				break;
			}
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onPrepareOptionsMenu(final Menu menu) {
		final MenuItem itemBrowser = menu.findItem(MENU_OPEN_IN_BROWSER);
		if (itemBrowser != null) {
			final boolean is_oauth = mAuthType == Accounts.AUTH_TYPE_OAUTH;
			itemBrowser.setVisible(is_oauth);
			itemBrowser.setEnabled(is_oauth);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public void onSaveInstanceState(final Bundle outState) {
		saveEditedText();
		setDefaultAPI();
		outState.putString(Accounts.API_URL_FORMAT, mAPIUrlFormat);
		outState.putInt(Accounts.AUTH_TYPE, mAuthType);
		outState.putBoolean(Accounts.SAME_OAUTH_SIGNING_URL, mSameOAuthSigningUrl);
        outState.putBoolean(Accounts.NO_VERSION_SUFFIX, mNoVersionSuffix);
		outState.putString(Accounts.CONSUMER_KEY, mConsumerKey);
		outState.putString(Accounts.CONSUMER_SECRET, mConsumerSecret);
		outState.putString(Accounts.SCREEN_NAME, mUsername);
		outState.putString(Accounts.PASSWORD, mPassword);
		outState.putLong(EXTRA_API_LAST_CHANGE, mAPIChangeTimestamp);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
		setSignInButton();
	}


	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		mResolver = getContentResolver();
		mApplication = TwittnukerApplication.getInstance(this);
		setContentView(R.layout.activity_sign_in);
//        setSupportActionBar((Toolbar) findViewById(R.id.tool_bar));
		if (savedInstanceState != null) {
			mAPIUrlFormat = savedInstanceState.getString(Accounts.API_URL_FORMAT);
			mAuthType = savedInstanceState.getInt(Accounts.AUTH_TYPE);
			mSameOAuthSigningUrl = savedInstanceState.getBoolean(Accounts.SAME_OAUTH_SIGNING_URL);
			mConsumerKey = trim(savedInstanceState.getString(Accounts.CONSUMER_KEY));
			mConsumerSecret = trim(savedInstanceState.getString(Accounts.CONSUMER_SECRET));
			mUsername = savedInstanceState.getString(Accounts.SCREEN_NAME);
			mPassword = savedInstanceState.getString(Accounts.PASSWORD);
			mAPIChangeTimestamp = savedInstanceState.getLong(EXTRA_API_LAST_CHANGE);
		}

		mUsernamePasswordContainer
				.setVisibility(mAuthType == Accounts.AUTH_TYPE_TWIP_O_MODE ? View.GONE : View.VISIBLE);
        mSignInSignUpContainer.setOrientation(mAuthType == Accounts.AUTH_TYPE_TWIP_O_MODE ? LinearLayout.VERTICAL
				: LinearLayout.HORIZONTAL);

		mEditUsername.setText(mUsername);
		mEditUsername.addTextChangedListener(this);
		mEditPassword.setText(mPassword);
		mEditPassword.addTextChangedListener(this);
        final Resources resources = getResources();
        final ColorStateList color = ColorStateList.valueOf(resources.getColor(R.color.material_light_green));
        ViewCompat.setBackgroundTintList(mSignInButton, color);
		setSignInButton();
	}

	private void doLogin() {
        if (mTask != null && mTask.getStatus() == AsyncTask.Status.RUNNING) {
			mTask.cancel(true);
		}
		saveEditedText();
		setDefaultAPI();
		final Configuration conf = getConfiguration();
        mTask = new SignInTask(this, conf, mUsername, mPassword, mAuthType, mAPIUrlFormat,
                mSameOAuthSigningUrl, mNoVersionSuffix);
        AsyncTaskUtils.executeTask(mTask);
	}

	private void doLogin(final Intent intent) {
		if (intent == null) return;
        if (mTask != null && mTask.getStatus() == AsyncTask.Status.RUNNING) {
			mTask.cancel(true);
		}
		saveEditedText();
		setDefaultAPI();
		final Configuration conf = getConfiguration();
		final String token = intent.getStringExtra(EXTRA_REQUEST_TOKEN);
		final String secret = intent.getStringExtra(EXTRA_REQUEST_TOKEN_SECRET);
		final String verifier = intent.getStringExtra(EXTRA_OAUTH_VERIFIER);
        mTask = new BrowserSignInTask(this, conf, token, secret, verifier, mAPIUrlFormat,
                mSameOAuthSigningUrl, mNoVersionSuffix);
        AsyncTaskUtils.executeTask(mTask);
	}

	private Configuration getConfiguration() {
		final ConfigurationBuilder cb = new ConfigurationBuilder();
		final boolean enable_gzip_compressing = mPreferences.getBoolean(KEY_GZIP_COMPRESSING, false);
		final boolean ignore_ssl_error = mPreferences.getBoolean(KEY_IGNORE_SSL_ERROR, false);
		final boolean enable_proxy = mPreferences.getBoolean(KEY_ENABLE_PROXY, false);
		cb.setHostAddressResolverFactory(new TwidereHostResolverFactory(mApplication));
        cb.setHttpClientFactory(new OkHttpClientFactory(mApplication));
        Utils.setClientUserAgent(this, mConsumerKey, mConsumerSecret, cb);
        final String apiUrlFormat = TextUtils.isEmpty(mAPIUrlFormat) ? DEFAULT_TWITTER_API_URL_FORMAT : mAPIUrlFormat;
		final String versionSuffix = mNoVersionSuffix ? null : "/1.1/";
        cb.setRestBaseURL(Utils.getApiUrl(apiUrlFormat, "api", versionSuffix));
        cb.setOAuthBaseURL(Utils.getApiUrl(apiUrlFormat, "api", "/oauth/"));
        cb.setUploadBaseURL(Utils.getApiUrl(apiUrlFormat, "upload", versionSuffix));
        cb.setOAuthAuthorizationURL(Utils.getApiUrl(apiUrlFormat, null, "/oauth/authorize"));
		if (!mSameOAuthSigningUrl) {
			cb.setSigningRestBaseURL(DEFAULT_SIGNING_REST_BASE_URL);
			cb.setSigningOAuthBaseURL(DEFAULT_SIGNING_OAUTH_BASE_URL);
			cb.setSigningUploadBaseURL(DEFAULT_SIGNING_UPLOAD_BASE_URL);
		}
		if (isEmpty(mConsumerKey) || isEmpty(mConsumerSecret)) {
			cb.setOAuthConsumerKey(TWITTER_CONSUMER_KEY_2);
			cb.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET_2);
		} else {
			cb.setOAuthConsumerKey(mConsumerKey);
			cb.setOAuthConsumerSecret(mConsumerSecret);
		}
		cb.setGZIPEnabled(enable_gzip_compressing);
		cb.setIgnoreSSLError(ignore_ssl_error);
		if (enable_proxy) {
			final String proxy_host = mPreferences.getString(KEY_PROXY_HOST, null);
			final int proxy_port = ParseUtils.parseInt(mPreferences.getString(KEY_PROXY_PORT, "-1"));
			if (!isEmpty(proxy_host) && proxy_port > 0) {
				cb.setHttpProxyHost(proxy_host);
				cb.setHttpProxyPort(proxy_port);
			}
		}
		return cb.build();
	}

	private void saveEditedText() {
		mUsername = ParseUtils.parseString(mEditUsername.getText());
		mPassword = ParseUtils.parseString(mEditPassword.getText());
	}

	private void setDefaultAPI() {
		final long apiLastChange = mPreferences.getLong(KEY_API_LAST_CHANGE, mAPIChangeTimestamp);
		final boolean defaultApiChanged = apiLastChange != mAPIChangeTimestamp;
		final String apiUrlFormat = getNonEmptyString(mPreferences, KEY_API_URL_FORMAT, null);
		final int authType = mPreferences.getInt(KEY_AUTH_TYPE, Accounts.AUTH_TYPE_OAUTH);
		final boolean sameOAuthSigningUrl = mPreferences.getBoolean(KEY_SAME_OAUTH_SIGNING_URL, false);
        final boolean noVersionSuffix = mPreferences.getBoolean(KEY_NO_VERSION_SUFFIX, false);
		final String consumerKey = getNonEmptyString(mPreferences, KEY_CONSUMER_KEY, TWITTER_CONSUMER_KEY_2);
		final String consumerSecret = getNonEmptyString(mPreferences, KEY_CONSUMER_SECRET, TWITTER_CONSUMER_SECRET_2);
		if (isEmpty(mAPIUrlFormat) || defaultApiChanged) {
			mAPIUrlFormat = apiUrlFormat;
		}
        if (defaultApiChanged) {
			mAuthType = authType;
		}
		if (defaultApiChanged) {
			mSameOAuthSigningUrl = sameOAuthSigningUrl;
		}
        if (defaultApiChanged) {
            mNoVersionSuffix = noVersionSuffix;
        }
		if (isEmpty(mConsumerKey) || defaultApiChanged) {
			mConsumerKey = consumerKey;
		}
		if (isEmpty(mConsumerSecret) || defaultApiChanged) {
			mConsumerSecret = consumerSecret;
		}
		if (defaultApiChanged) {
			mAPIChangeTimestamp = apiLastChange;
		}
	}

	private void setSignInButton() {
		mSignInButton.setEnabled(mEditPassword.getText().length() > 0 && mEditUsername.getText().length() > 0
				|| mAuthType == Accounts.AUTH_TYPE_TWIP_O_MODE);
	}

    void onSignInResult(final SignInResponse result) {
        final FragmentManager fm = getSupportFragmentManager();
        final Fragment f = fm.findFragmentByTag(FRAGMENT_TAG_SIGN_IN_PROGRESS);
        if (f instanceof DialogFragment) {
            ((DialogFragment) f).dismiss();
        }
		if (result != null) {
			if (result.succeed) {
				final ContentValues values;
				switch (result.auth_type) {
					case Accounts.AUTH_TYPE_BASIC: {
                        values = createAccount(result.conf, result.basic_username,
								result.basic_password, result.user, result.color,
								result.api_url_format, result.no_version_suffix);
						break;
					}
					case Accounts.AUTH_TYPE_TWIP_O_MODE: {
                        values = ContentValuesCreator.createAccount(result.conf, result.user, result.color,
								result.api_url_format, result.no_version_suffix);
						break;
					}
					case Accounts.AUTH_TYPE_OAUTH:
					case Accounts.AUTH_TYPE_XAUTH: {
                        values = ContentValuesCreator.createAccount(result.conf, result.access_token,
								result.user, result.auth_type, result.color, result.api_url_format,
								result.same_oauth_signing_url, result.no_version_suffix);
						break;
					}
					default: {
						values = null;
					}
				}
				if (values != null) {
					mResolver.insert(Accounts.CONTENT_URI, values);
				}
                final long loggedId = result.user.getId();
				final Intent intent = new Intent(this, HomeActivity.class);
                intent.putExtra(EXTRA_REFRESH_IDS, new long[]{loggedId});
				intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivity(intent);
				finish();
			} else if (result.already_logged_in) {
                Toast.makeText(this, R.string.error_already_logged_in, Toast.LENGTH_SHORT).show();
			} else {
				if (result.exception instanceof AuthenticityTokenException) {
                    Toast.makeText(this, R.string.wrong_api_key, Toast.LENGTH_SHORT).show();
				} else if (result.exception instanceof WrongUserPassException) {
                    Toast.makeText(this, R.string.wrong_username_password, Toast.LENGTH_SHORT).show();
				} else if (result.exception instanceof AuthenticationException) {
					showErrorMessage(this, getString(R.string.action_signing_in), result.exception.getCause(), true);
				} else {
					showErrorMessage(this, getString(R.string.action_signing_in), result.exception, true);
				}
			}
		}
		setSignInButton();
	}

	void onSignInStart() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
        		if (isFinishing()) return;
                final FragmentManager fm = getSupportFragmentManager();
                final FragmentTransaction ft = fm.beginTransaction();
                final SupportProgressDialogFragment fragment = new SupportProgressDialogFragment();
        		fragment.setCancelable(false);
                fragment.show(ft, FRAGMENT_TAG_SIGN_IN_PROGRESS);
			}
        });
    }

    public static abstract class AbstractSignInTask extends AsyncTask<Object, Object, SignInResponse> {

		protected final Configuration conf;
		protected final SignInActivity callback;

		public AbstractSignInTask(final SignInActivity callback, final Configuration conf) {
			this.conf = conf;
			this.callback = callback;
		}

		@Override
        protected void onPostExecute(final SignInResponse result) {
			if (callback != null) {
				callback.onSignInResult(result);
			}
		}

		@Override
		protected void onPreExecute() {
			if (callback != null) {
				callback.onSignInStart();
			}
		}

		int analyseUserProfileColor(final User user) throws TwitterException {
			if (user == null) throw new TwitterException("Unable to get user info");
            return ParseUtils.parseColor("#" + user.getProfileLinkColor(), Color.TRANSPARENT);
        }

	}

	public static class BrowserSignInTask extends AbstractSignInTask {

		private final Configuration conf;
		private final String request_token, request_token_secret, oauth_verifier;

		private final Context context;
		private final String api_url_format;
        private final boolean same_oauth_signing_url, no_version_suffix;

        public BrowserSignInTask(final SignInActivity context, final Configuration conf,
                                 final String request_token, final String request_token_secret,
                                 final String oauth_verifier, final String api_url_format,
                                 final boolean same_oauth_signing_url, final boolean no_version_suffix) {
			super(context, conf);
			this.context = context;
			this.conf = conf;
			this.request_token = request_token;
			this.request_token_secret = request_token_secret;
			this.oauth_verifier = oauth_verifier;
			this.api_url_format = api_url_format;
			this.same_oauth_signing_url = same_oauth_signing_url;
            this.no_version_suffix = no_version_suffix;
		}

		@Override
        protected SignInResponse doInBackground(final Object... params) {
			try {
				final Twitter twitter = new TwitterFactory(conf).getInstance();
				final AccessToken access_token = twitter.getOAuthAccessToken(new RequestToken(conf, request_token,
						request_token_secret), oauth_verifier);
				final long userId = access_token.getUserId();
                if (userId <= 0) return new SignInResponse(false, false, null);
				final User user = twitter.verifyCredentials();
                if (isUserLoggedIn(context, userId)) return new SignInResponse(true, false, null);
                final int color = analyseUserProfileColor(user);
                return new SignInResponse(conf, access_token, user, Accounts.AUTH_TYPE_OAUTH, color,
                        api_url_format, same_oauth_signing_url, no_version_suffix);
			} catch (final TwitterException e) {
                return new SignInResponse(false, false, e);
			}
		}
	}

	public static class SignInMethodIntroductionDialogFragment extends BaseSupportDialogFragment {

        @NonNull
		@Override
		public Dialog onCreateDialog(final Bundle savedInstanceState) {
            final Context wrapped = ThemeUtils.getDialogThemedContext(getActivity());
            final AlertDialog.Builder builder = new AlertDialog.Builder(wrapped);
			builder.setTitle(R.string.sign_in_method_introduction_title);
			builder.setMessage(R.string.sign_in_method_introduction);
			builder.setPositiveButton(android.R.string.ok, null);
			return builder.create();
		}

	}

	public static class SignInTask extends AbstractSignInTask {

		private final Configuration conf;
		private final String username, password;
		private final int auth_type;

		private final Context context;
		private final String api_url_format;
        private final boolean same_oauth_signing_url, no_version_suffix;

        public SignInTask(final SignInActivity context, final Configuration conf,
                          final String username, final String password, final int auth_type,
                          final String api_url_format, final boolean same_oauth_signing_url,
                          final boolean no_version_suffix) {
			super(context, conf);
			this.context = context;
			this.conf = conf;
			this.username = username;
			this.password = password;
			this.auth_type = auth_type;
			this.api_url_format = api_url_format;
			this.same_oauth_signing_url = same_oauth_signing_url;
            this.no_version_suffix = no_version_suffix;
		}

		@Override
        protected SignInResponse doInBackground(final Object... params) {
			try {
				switch (auth_type) {
					case Accounts.AUTH_TYPE_OAUTH:
						return authOAuth();
					case Accounts.AUTH_TYPE_XAUTH:
						return authxAuth();
					case Accounts.AUTH_TYPE_BASIC:
						return authBasic();
					case Accounts.AUTH_TYPE_TWIP_O_MODE:
						return authTwipOMode();
				}
				return authOAuth();
			} catch (final TwitterException e) {
				e.printStackTrace();
                return new SignInResponse(false, false, e);
			} catch (final AuthenticationException e) {
				e.printStackTrace();
                return new SignInResponse(false, false, e);
			}
		}

        private SignInResponse authBasic() throws TwitterException {
			final Twitter twitter = new TwitterFactory(conf).getInstance(new BasicAuthorization(username, password));
			final User user = twitter.verifyCredentials();
			final long user_id = user.getId();
            if (user_id <= 0) return new SignInResponse(false, false, null);
            if (isUserLoggedIn(context, user_id)) return new SignInResponse(true, false, null);
            final int color = analyseUserProfileColor(user);
            return new SignInResponse(conf, username, password, user, color, api_url_format,
                    no_version_suffix);
		}

        private SignInResponse authOAuth() throws AuthenticationException, TwitterException {
			final Twitter twitter = new TwitterFactory(conf).getInstance();
			final OAuthPasswordAuthenticator authenticator = new OAuthPasswordAuthenticator(twitter);
			final AccessToken access_token = authenticator.getOAuthAccessToken(username, password);
			final long user_id = access_token.getUserId();
            if (user_id <= 0) return new SignInResponse(false, false, null);
			final User user = twitter.verifyCredentials();
            if (isUserLoggedIn(context, user_id)) return new SignInResponse(true, false, null);
            final int color = analyseUserProfileColor(user);
            return new SignInResponse(conf, access_token, user, Accounts.AUTH_TYPE_OAUTH, color,
                    api_url_format, same_oauth_signing_url, no_version_suffix);
		}

        private SignInResponse authTwipOMode() throws TwitterException {
			final Twitter twitter = new TwitterFactory(conf).getInstance(new TwipOModeAuthorization());
			final User user = twitter.verifyCredentials();
			final long user_id = user.getId();
            if (user_id <= 0) return new SignInResponse(false, false, null);
            if (isUserLoggedIn(context, user_id)) return new SignInResponse(true, false, null);
            final int color = analyseUserProfileColor(user);
            return new SignInResponse(conf, user, color, api_url_format, no_version_suffix);
		}

        private SignInResponse authxAuth() throws TwitterException {
			final Twitter twitter = new TwitterFactory(conf).getInstance();
            final AccessToken accessToken = twitter.getOAuthAccessToken(username, password);
			final User user = twitter.verifyCredentials();
			final long user_id = user.getId();
            if (user_id <= 0) return new SignInResponse(false, false, null);
            if (isUserLoggedIn(context, user_id)) return new SignInResponse(true, false, null);
            final int color = analyseUserProfileColor(user);
            return new SignInResponse(conf, accessToken, user, Accounts.AUTH_TYPE_XAUTH, color,
                    api_url_format, same_oauth_signing_url, no_version_suffix);
		}

	}

    static class SignInResponse {

		public final boolean already_logged_in, succeed;
		public final Exception exception;
		public final Configuration conf;
		public final String basic_username, basic_password;
		public final AccessToken access_token;
		public final User user;
		public final int auth_type, color;
		public final String api_url_format;
        public final boolean same_oauth_signing_url, no_version_suffix;

        public SignInResponse(final boolean already_logged_in, final boolean succeed, final Exception exception) {
            this(already_logged_in, succeed, exception, null, null, null, null, null, 0, 0, null, false, false);
		}

        public SignInResponse(final boolean already_logged_in, final boolean succeed, final Exception exception,
				final Configuration conf, final String basic_username, final String basic_password,
				final AccessToken access_token, final User user, final int auth_type, final int color,
                              final String api_url_format, final boolean same_oauth_signing_url, final boolean no_version_suffix) {
			this.already_logged_in = already_logged_in;
			this.succeed = succeed;
			this.exception = exception;
			this.conf = conf;
			this.basic_username = basic_username;
			this.basic_password = basic_password;
			this.access_token = access_token;
			this.user = user;
			this.auth_type = auth_type;
			this.color = color;
			this.api_url_format = api_url_format;
			this.same_oauth_signing_url = same_oauth_signing_url;
            this.no_version_suffix = no_version_suffix;
		}

        public SignInResponse(final Configuration conf, final AccessToken access_token, final User user,
                              final int auth_type, final int color, final String api_url_format,
                              final boolean same_oauth_signing_url, final boolean no_version_suffix) {
			this(false, true, null, conf, null, null, access_token, user, auth_type, color, api_url_format,
                    same_oauth_signing_url, no_version_suffix);
		}

        public SignInResponse(final Configuration conf, final String basic_username, final String basic_password,
                              final User user, final int color, final String api_url_format,
                              final boolean no_version_suffix) {
			this(false, true, null, conf, basic_username, basic_password, null, user, Accounts.AUTH_TYPE_BASIC, color,
                    api_url_format, false, no_version_suffix);
		}

        public SignInResponse(final Configuration conf, final User user, final int color,
                              final String api_url_format, final boolean no_version_suffix) {
			this(false, true, null, conf, null, null, null, user, Accounts.AUTH_TYPE_TWIP_O_MODE, color,
                    api_url_format, false, no_version_suffix);
		}
	}
}