export function OpenLCALogoSVG({
  className,
  width,
  height,
}: {
  width?: number;
  height?: number;
  className?: string;
}) {
  return (
    <img
      src="images/openlca-128.png"
      alt="openLCA"
      width={width}
      height={height}
      className={className}
    />
  );
}
