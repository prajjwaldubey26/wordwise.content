export default function SoftCard({ children, className = '', as: Tag = 'div', ...rest }) {
  return (
    <Tag className={`soft-card ${className}`.trim()} {...rest}>
      {children}
    </Tag>
  );
}
