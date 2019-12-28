/*
 * Copyright 2016-2019 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ciscowebex.androidsdk.membership;

import java.util.Date;

import com.cisco.spark.android.model.ParticipantRoomProperties;
import com.cisco.spark.android.model.Person;
import com.cisco.spark.android.model.Verb;
import com.cisco.spark.android.model.conversation.Activity;
import com.cisco.spark.android.model.conversation.Conversation;
import com.ciscowebex.androidsdk.utils.WebexId;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * Membership contents.
 *
 * @since 0.1
 */
public class Membership {

    @SerializedName("id")
    private String _id;

    @SerializedName("personId")
    private String _personId;

    @SerializedName("personEmail")
    private String _personEmail;

    @SerializedName("personDisplayName")
    private String _personDisplayName;

    @SerializedName("personOrgId")
    private String _personOrgId;

    @SerializedName(value = "roomId", alternate = "spaceId")
    private String _spaceId;

    @SerializedName("isModerator")
    private boolean _isModerator;

    @SerializedName("isMonitor")
    @Deprecated
    private boolean _isMonitor;

    @SerializedName("created")
    private Date _created;

    protected Membership(Conversation conversation, Person person) {
        _id = new WebexId(WebexId.Type.MEMBERSHIP_ID, person.getId() + ":" + conversation.getId()).toHydraId();
        _spaceId = new WebexId(WebexId.Type.ROOM_ID, conversation.getId()).toHydraId();
        _personId = new WebexId(WebexId.Type.PEOPLE_ID, person.getId()).toHydraId();
        _personEmail = person.getEmail();
        _personDisplayName = person.getDisplayName();
        _personOrgId = new WebexId(WebexId.Type.ORGANIZATION_ID, person.getOrgId()).toHydraId();
        ParticipantRoomProperties roomProperties = person.getRoomProperties();
        if (roomProperties != null) {
            _isModerator = roomProperties.isModerator();
            _isMonitor = _isModerator;
        }
        _created = null; // created is not available in the conversations payload
    }

    protected Membership(Activity activity) {
        this._created = activity.getPublished();
        if (activity.getVerb().equals(Verb.hide)) {
            this._spaceId = new WebexId(WebexId.Type.ROOM_ID, activity.getObject().getId()).toHydraId();
        } else {
            this._spaceId = new WebexId(WebexId.Type.ROOM_ID, activity.getTarget().getId()).toHydraId();
        }
        Person person = null;
        if (activity.getVerb().equals(Verb.acknowledge)) {
            person = activity.getActor();
        } else if (activity.getObject() instanceof Person) {
            person = (Person) activity.getObject();
        }
        if (null != person) {
            this._id = new WebexId(WebexId.Type.MEMBERSHIP_ID, person.getId() + ":" + WebexId.translate(this._spaceId)).toHydraId();
            this._personId = new WebexId(WebexId.Type.PEOPLE_ID, person.getId()).toHydraId();
            this._personEmail = person.getEmail();
            this._personDisplayName = person.getDisplayName();
            this._personOrgId = new WebexId(WebexId.Type.ORGANIZATION_ID, person.getOrgId()).toHydraId();
            this._isModerator = person.getRoomProperties() != null && person.getRoomProperties().isModerator();
            this._isMonitor = _isModerator;
        }
    }

    /**
     * @return The id of this membership.
     * @since 0.1
     */
    public String getId() {
        return _id;
    }

    /**
     * @return The id of the person.
     * @since 0.1
     */
    public String getPersonId() {
        return _personId;
    }

    /**
     * @return The email address of the person.
     * @since 0.1
     */
    public String getPersonEmail() {
        return _personEmail;
    }

    /**
     * @return The display name of the person.
     * @since 0.1
     */
    public String getPersonDisplayName() {
        return _personDisplayName;
    }

    /**
     * @return The id of the space.
     * @since 0.1
     */
    public String getSpaceId() {
        return _spaceId;
    }

    /**
     * @return True if this member is a moderator of the space in this membership. Otherwise false.
     * @since 0.1
     */
    public boolean isModerator() {
        return _isModerator;
    }

    /**
     * @return True if this member is a monitor of the space in this membership. Otherwise false.
     * @deprecated
     * @since 0.1
     */
    @Deprecated
    public boolean isMonitor() {
        return _isMonitor;
    }

    /**
     * @return The timestamp that the membership being created.
     * @since 0.1
     */
    public Date getCreated() {
        return _created;
    }

    /**
     * @return The personOrgId name of the person
     * @since 1.4
     */
    public String getPersonOrgId() {
        return _personOrgId;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
