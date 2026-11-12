// Server-side catalogue. Prices live here, never in the client.

export const GIFTS = [
  { id: "rose", name: "Rose", description: "A classic rose for the stage.", cost: 10, icon: "heart", tint: "#FF738F" },
  { id: "star", name: "Lucky Star", description: "A little luck for the next round.", cost: 25, icon: "star", tint: "#00CCF9" },
  { id: "box", name: "Gift Box", description: "A small surprise for a friend.", cost: 50, icon: "gift", tint: "#50DCA2" },
  { id: "bolt", name: "Power Bolt", description: "Boost the energy in the room.", cost: 100, icon: "bolt", tint: "#9B8CFF" },
  { id: "coins", name: "Coin Bag", description: "Share a handful of coins.", cost: 200, icon: "coin", tint: "#FF6512" },
  { id: "crown", name: "Golden Crown", description: "For the room royalty.", cost: 500, icon: "crown", tint: "#F7E25A" },
];

export const COIN_PACKS = [
  { id: "coins_100", coins: 100, bonus: 0, priceMinor: 9900 },
  { id: "coins_500", coins: 500, bonus: 50, priceMinor: 44900 },
  { id: "coins_1200", coins: 1200, bonus: 150, priceMinor: 89900 },
  { id: "coins_3000", coins: 3000, bonus: 500, priceMinor: 199900 },
  { id: "coins_6000", coins: 6000, bonus: 1200, priceMinor: 379900 },
  { id: "coins_12000", coins: 12000, bonus: 3000, priceMinor: 749900 },
];
