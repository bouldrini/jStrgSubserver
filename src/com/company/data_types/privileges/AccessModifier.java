package com.company.data_types.privileges;

/**
 * can be used to pass access rights between functions and classes. booleans are nullable to indicate a not set state.
 */
public class AccessModifier {

    private Boolean m_read;
    private Boolean m_write;
    private Boolean m_delete;

    private Boolean m_invite;
    private Boolean m_owner;


    public Boolean read() {
        return m_read;
    }

    public void set_read(Boolean m_read) {
        this.m_read = m_read;
    }

    public Boolean write() {
        return m_write;
    }

    public void set_write(Boolean m_write) {
        this.m_write = m_write;
    }

    public Boolean delete() {
        return m_delete;
    }

    public void set_delete(Boolean m_delete) {
        this.m_delete = m_delete;
    }

    public Boolean invite() {
        return m_invite;
    }

    public void set_invite(Boolean m_invite) {
        this.m_invite = m_invite;
    }

    public Boolean owner() {
        return m_owner;
    }

    public void set_owner(Boolean m_owner) {
        this.m_owner = m_owner;
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder("<AccessModifier::{");

        string.append(" read: ");
        string.append(read() == null ? " not set" : read());
        string.append(" write: ");
        string.append(write() == null ? " not set" : write());
        string.append(" delete: ");
        string.append(delete() == null ? " not set" : delete());
        string.append(" invite: ");
        string.append(invite() == null ? " not set" : invite());

        string.append(" }>");
        return string.toString();
    }
}
