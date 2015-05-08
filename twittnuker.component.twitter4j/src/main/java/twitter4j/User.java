/*
 * Copyright 2007 Yusuke Yamamoto
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package twitter4j;

import java.io.Serializable;
import java.util.Date;

/**
 * A data interface representing Basic user information element
 * 
 * @author Yusuke Yamamoto - yusuke at mac.com
 */
public interface User extends Comparable<User>, TwitterResponse, Serializable {
	Date getCreatedAt();

	boolean isDefaultProfile();

	/**
	 * Returns the description of the user
	 * 
	 * @return the description of the user
	 */
	String getDescription();

	UrlEntity[] getDescriptionEntities();

	long getFavouritesCount();

	boolean isFollowedBy();

	/**
	 * Returns the number of followers
	 * 
	 * @return the number of followers
	 * @since Twitter4J 1.0.4
	 */
	long getFollowersCount();

	long getFriendsCount();

	boolean hasCustomTimelines();

	/**
	 * Returns the id of the user
	 * 
	 * @return the id of the user
	 */
	long getId();

	/**
	 * Returns the preferred language of the user
	 * 
	 * @return the preferred language of the user
	 * @since Twitter4J 2.1.2
	 */
	String getLang();

	/**
	 * Returns the number of public lists the user is listed on, or -1 if the
	 * count is unavailable.
	 * 
	 * @return the number of public lists the user is listed on.
	 * @since Twitter4J 2.1.4
	 */
	long getListedCount();

	/**
	 * Returns the location of the user
	 * 
	 * @return the location of the user
	 */
	String getLocation();

	/**
	 * Returns the name of the user
	 * 
	 * @return the name of the user
	 */
	String getName();

	boolean isNeedsPhoneVerification();

	boolean isNotifications();

	String getProfileBackgroundColor();

	String getProfileBackgroundImageUrl();

	String getProfileBackgroundImageUrlHttps();

	String getProfileBannerImageUrl();

	/**
	 * Returns the profile image url of the user
	 * 
	 * @return the profile image url of the user
	 */
    String getProfileImageUrl();

	/**
	 * Returns the profile image url of the user, served over SSL
	 * 
	 * @return the profile image url of the user, served over SSL
	 */
	String getProfileImageUrlHttps();

	String getProfileLinkColor();

	String getProfileLocation();

	String getProfileSidebarBorderColor();

	String getProfileSidebarFillColor();

	String getProfileTextColor();

	/**
	 * Returns the screen name of the user
	 * 
	 * @return the screen name of the user
	 */
	String getScreenName();

	/**
	 * Returns the current status of the user<br>
	 * This can be null if the instance if from Status.getUser().
	 * 
	 * @return current status of the user
	 * @since Twitter4J 2.1.1
	 */
	Status getStatus();

	long getStatusesCount();

	long getMediaCount();

	boolean isSuspended();

	String getTimeZone();

	/**
	 * Returns the url of the user
	 * 
	 * @return the url of the user
	 */
    String getUrl();

	UrlEntity[] getUrlEntities();

	int getUtcOffset();

	boolean canMediaTag();

	/**
	 * Tests if the user is enabling contributors
	 * 
	 * @return if the user is enabling contributors
	 * @since Twitter4J 2.1.2
	 */
	boolean isContributorsEnabled();

	boolean isDefaultProfileImage();

	boolean isFollowing();

	/**
	 * Returns true if the authenticating user has requested to follow this
	 * user, otherwise false.
	 * 
	 * @return true if the authenticating user has requested to follow this
	 *         user.
	 * @since Twitter4J 2.1.4
	 */
	boolean isFollowRequestSent();

	/**
	 * @return the user is enabling geo location
	 * @since Twitter4J 2.0.10
	 */
	boolean isGeoEnabled();

	boolean isProfileBackgroundTiled();

	boolean isProfileUseBackgroundImage();

	/**
	 * Test if the user status is protected
	 * 
	 * @return true if the user status is protected
	 */
	boolean isProtected();

	boolean isTranslationEnabled();

	/**
	 * @return returns true if the user is a translator
	 * @since Twitter4J 2.1.9
	 */
	boolean isTranslator();

	/**
	 * @return returns true if the user is a verified celebrity
	 * @since Twitter4J 2.0.10
	 */
	boolean isVerified();

}