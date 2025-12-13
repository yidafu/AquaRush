export function formatAdminTableAmount(amount?: number | null) {
  if (typeof amount != 'number') {
    return '0'
  }
  return (amount / 100).toFixed(2)
}
