package ma.mizaje.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Image d'une pub partenaire, à part pour ne pas la charger avec la liste des partenaires. */
@Entity
@Table(name = "partner_pictures")
public class PartnerPicture {

	/** Identifiant du partenaire. */
	@Id
	private Long partnerId;

	@Column(nullable = false, length = 3_000_000)
	private byte[] data;

	protected PartnerPicture() {
	}

	public PartnerPicture(Long partnerId, byte[] data) {
		this.partnerId = partnerId;
		this.data = data;
	}

	public void setData(byte[] data) { this.data = data; }

	public byte[] getData() { return data; }
}
