package ma.mizaje.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Image d'une pub de la semaine, à part pour ne pas la charger avec la liste des pubs. */
@Entity
@Table(name = "weekly_ad_pictures")
public class WeeklyAdPicture {

	/** Identifiant de la pub. */
	@Id
	private Long adId;

	@Column(nullable = false, length = 3_000_000)
	private byte[] data;

	protected WeeklyAdPicture() {
	}

	public WeeklyAdPicture(Long adId, byte[] data) {
		this.adId = adId;
		this.data = data;
	}

	public void setData(byte[] data) { this.data = data; }

	public byte[] getData() { return data; }
}
