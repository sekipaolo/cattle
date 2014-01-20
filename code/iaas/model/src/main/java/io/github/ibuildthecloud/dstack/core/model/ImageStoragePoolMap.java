/**
 * This class is generated by jOOQ
 */
package io.github.ibuildthecloud.dstack.core.model;

/**
 * This class is generated by jOOQ.
 */
@javax.annotation.Generated(value    = { "http://www.jooq.org", "3.3.0" },
                            comments = "This class is generated by jOOQ")
@java.lang.SuppressWarnings({ "all", "unchecked", "rawtypes" })
@javax.persistence.Entity
@javax.persistence.Table(name = "image_storage_pool_map", schema = "dstack")
public interface ImageStoragePoolMap extends java.io.Serializable {

	/**
	 * Setter for <code>dstack.image_storage_pool_map.id</code>. 
	 */
	public void setId(java.lang.Long value);

	/**
	 * Getter for <code>dstack.image_storage_pool_map.id</code>. 
	 */
	@javax.persistence.Id
	@javax.persistence.Column(name = "id", unique = true, nullable = false, precision = 19)
	public java.lang.Long getId();

	/**
	 * Setter for <code>dstack.image_storage_pool_map.image_id</code>. 
	 */
	public void setImageId(java.lang.Long value);

	/**
	 * Getter for <code>dstack.image_storage_pool_map.image_id</code>. 
	 */
	@javax.persistence.Column(name = "image_id", nullable = false, precision = 19)
	public java.lang.Long getImageId();

	/**
	 * Setter for <code>dstack.image_storage_pool_map.storage_pool_id</code>. 
	 */
	public void setStoragePoolId(java.lang.Long value);

	/**
	 * Getter for <code>dstack.image_storage_pool_map.storage_pool_id</code>. 
	 */
	@javax.persistence.Column(name = "storage_pool_id", nullable = false, precision = 19)
	public java.lang.Long getStoragePoolId();

	/**
	 * Setter for <code>dstack.image_storage_pool_map.state</code>. 
	 */
	public void setState(java.lang.String value);

	/**
	 * Getter for <code>dstack.image_storage_pool_map.state</code>. 
	 */
	@javax.persistence.Column(name = "state", nullable = false, length = 255)
	public java.lang.String getState();

	/**
	 * Setter for <code>dstack.image_storage_pool_map.uri</code>. 
	 */
	public void setUri(java.lang.String value);

	/**
	 * Getter for <code>dstack.image_storage_pool_map.uri</code>. 
	 */
	@javax.persistence.Column(name = "uri", length = 255)
	public java.lang.String getUri();

	/**
	 * Setter for <code>dstack.image_storage_pool_map.created</code>. 
	 */
	public void setCreated(java.sql.Timestamp value);

	/**
	 * Getter for <code>dstack.image_storage_pool_map.created</code>. 
	 */
	@javax.persistence.Column(name = "created")
	public java.sql.Timestamp getCreated();

	/**
	 * Setter for <code>dstack.image_storage_pool_map.removed</code>. 
	 */
	public void setRemoved(java.sql.Timestamp value);

	/**
	 * Getter for <code>dstack.image_storage_pool_map.removed</code>. 
	 */
	@javax.persistence.Column(name = "removed")
	public java.sql.Timestamp getRemoved();

	/**
	 * Setter for <code>dstack.image_storage_pool_map.remove_time</code>. 
	 */
	public void setRemoveTime(java.sql.Timestamp value);

	/**
	 * Getter for <code>dstack.image_storage_pool_map.remove_time</code>. 
	 */
	@javax.persistence.Column(name = "remove_time")
	public java.sql.Timestamp getRemoveTime();

	/**
	 * Setter for <code>dstack.image_storage_pool_map.remove_locked</code>. 
	 */
	public void setRemoveLocked(java.lang.Boolean value);

	/**
	 * Getter for <code>dstack.image_storage_pool_map.remove_locked</code>. 
	 */
	@javax.persistence.Column(name = "remove_locked", nullable = false, precision = 1)
	public java.lang.Boolean getRemoveLocked();

	// -------------------------------------------------------------------------
	// FROM and INTO
	// -------------------------------------------------------------------------

	/**
	 * Load data from another generated Record/POJO implementing the common interface ImageStoragePoolMap
	 */
	public void from(io.github.ibuildthecloud.dstack.core.model.ImageStoragePoolMap from);

	/**
	 * Copy data into another generated Record/POJO implementing the common interface ImageStoragePoolMap
	 */
	public <E extends io.github.ibuildthecloud.dstack.core.model.ImageStoragePoolMap> E into(E into);
}