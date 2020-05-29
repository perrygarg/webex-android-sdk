/**
 * Copyright 2016-2020 Cisco Systems Inc
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * <p>
 * An Single sign-on [SSO](https://help.webex.com/docs/DOC-9143#reference_E9B2CEDE975E4CD311C56D9B0EF2476C)
 * based authentication strategy used to authenticate a user on Cisco Webex.
 * <p>
 * - see: [Cisco Webex Integration](https://developer.webex.com/authentication.html)
 * - since: 1.3.0
 */

package com.ciscowebex.androidsdk.auth;

import java.util.Map;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.webkit.WebView;
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.auth.internal.OAuthLauncher;
import com.ciscowebex.androidsdk.internal.ResultImpl;
import com.ciscowebex.androidsdk.internal.Service;
import com.github.benoitdion.ln.Ln;
import me.helloworld.utils.Checker;

/**
 * An Single sign-on <a href="https://help.webex.com/docs/DOC-9143#reference_E9B2CEDE975E4CD311C56D9B0EF2476C">SSO</a> based authentication strategy used to authenticate a user on Cisco Webex.
 *
 * @see <a href="https://developer.webex.com/authentication.html">Cisco Webex Integration</a>
 * @since 1.3.0
 */
public class SSOAuthenticator implements Authenticator {

    private OAuthAuthenticator authenticator;

    private OAuthLauncher launcher;

    private String email;

    private String identityProviderUri;

    private Map<String, String> additionalQueryItems;

    /**
     * Creates a new OAuth authentication strategy
     *
     * @param clientId the OAuth client id
     * @param clientSecret the OAuth client secret
     * @param scope space-separated string representing which permissions the application needs
     * @param redirectUri the redirect URI that will be called when completing the authentication. This must match the redirect URI registered to your clientId.
     * @param email The webex email address of the SSO user.
     * @param identityProviderUri parameter identityProviderUri: the URI that will handle authentication claims with webex service on behalf of the hosting application.
     * @param queryItems a collection of additional query items to be appended to the identityProviderUri.
     * @see <a href="https://developer.webex.com/authentication.html">Cisco Webex Integration</a>
     * @since 1.3.0
     */
    @Deprecated
    public SSOAuthenticator(@NonNull String clientId, @NonNull String clientSecret, @NonNull String scope, @NonNull String redirectUri,
                            @NonNull String email, @NonNull String identityProviderUri, @Nullable Map<String, String> queryItems) {
        authenticator = new OAuthAuthenticator(clientId, clientSecret, scope, redirectUri);
        launcher = new OAuthLauncher();
        this.email = email;
        this.identityProviderUri = identityProviderUri;
        this.additionalQueryItems = queryItems;
    }

    public void afterAssociated() {
        authenticator.afterAssociated();
    }

    /**
     * @see Authenticator
     */
    @Override
    public boolean isAuthorized() {
        return authenticator.isAuthorized();
    }

    /**
     * @see Authenticator
     */
    @Override
    public void deauthorize() {
        authenticator.deauthorize();
    }

    /**
     * Brings up a web-based authorization view and directs the user through the OAuth process.
     *
     * @param view the web view for the authorization by the end user.
     * @param handler the completion handler will be called when authentication is complete, the error to indicate if the authentication process was successful.
     * @since 1.3.0
     */
    public void authorize(@NonNull WebView view, @NonNull CompletionHandler<Void> handler) {
        launcher.launchOAuthView(view, buildCodeGrantUrl(), authenticator.getRedirectUri(), result -> {
            Ln.d("Authorize: " + result);
            String code = result.getData();
            if (!Checker.isEmpty(code)) {
                authenticator.authorize(code, handler);
            } else {
                ResultImpl.errorInMain(handler, result);
            }
        });
    }

    /**
     * @see Authenticator
     */
    @Override
    public void getToken(CompletionHandler<String> handler) {
        authenticator.getToken(handler);
    }

    @Override
    public void refreshToken(CompletionHandler<String> handler) {
        authenticator.refreshToken(handler);
    }
    
    /** Create the authorizationUrl by taking the original url and redirecting the request through the
     * provided identity provider uri. Once the identity provider has validated the claim with Cisco Services it will
     * redirect back to continue a slimmed down version of oAuth authentication flow which has prefilled the user webex
     * id.
     *
     * This flow only interacts with the user if they need to explicitly need to provide permissions to allow webex to
     * use their account.
     */
    private String buildCodeGrantUrl() {
        Uri.Builder orginalUrl = Uri.parse(Service.Hydra.endpoint(null)).buildUpon();
        orginalUrl.appendPath("authorize")
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("client_id", authenticator.getClientId())
                .appendQueryParameter("redirect_uri", authenticator.getRedirectUri())
                .appendQueryParameter("scope", authenticator.getScope())
                .appendQueryParameter("state", "androidsdkstate");
        if (email != null && !email.isEmpty()) {
            // Extend the original authorization url with the email parameter.
            orginalUrl.appendQueryParameter("email", email);
        }

        Uri.Builder builder = Uri.parse(identityProviderUri).buildUpon();
        // Provide the modified authorizationUri as a query parameter to the identity provider.
        builder.appendQueryParameter("returnTo", orginalUrl.toString());
        if (additionalQueryItems != null) {
            // Append any additional query parameters for the identity provider.
            for (Map.Entry<String, String> entry : additionalQueryItems.entrySet()) {
                builder.appendQueryParameter(entry.getKey(), entry.getValue());
            }
        }
        Ln.d(builder.toString());
        return builder.toString();
    }
}
