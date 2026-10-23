package ma.mizaje.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Réglage simple de l'application (clé → valeur), modifiable par l'administrateur. */
@Entity
@Table(name = "app_settings")
public class AppSetting {

	@Id
	@Column(name = "setting_key", length = 60)
	private String key;

	@Column(name = "setting_value", nullable = false, length = 200)
	private String value;

	protected AppSetting() {
	}

	public AppSetting(String key, String value) {
		this.key = key;
		this.value = value;
	}

	public String getKey() { return key; }
	public String getValue() { return value; }
	public void setValue(String value) { this.value = value; }
}
