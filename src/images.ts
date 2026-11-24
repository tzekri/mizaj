/**
 * Réduit une image choisie dans l'administration (côté le plus long : {@code max} px au plus, JPEG) :
 * légère pour la base de données et rapide à afficher sur les écrans.
 */
export function shrink(file: File, max = 1600): Promise<string> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => {
      const scale = Math.min(1, max / Math.max(img.width, img.height));
      const canvas = document.createElement('canvas');
      canvas.width = Math.round(img.width * scale);
      canvas.height = Math.round(img.height * scale);
      canvas.getContext('2d')!.drawImage(img, 0, 0, canvas.width, canvas.height);
      URL.revokeObjectURL(img.src);
      resolve(canvas.toDataURL('image/jpeg', 0.86));
    };
    img.onerror = () => reject(new Error('Image illisible : choisissez une photo JPEG, PNG ou WebP.'));
    img.src = URL.createObjectURL(file);
  });
}
