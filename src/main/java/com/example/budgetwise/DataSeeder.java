package com.example.budgetwise;

import com.example.budgetwise.product.entity.DailyPriceRecord;
import com.example.budgetwise.product.entity.PriceReport;
import com.example.budgetwise.product.entity.ProductInfo;
import com.example.budgetwise.market.entity.MarketLocation;
import com.example.budgetwise.market.repository.MarketLocationRepository;
import com.example.budgetwise.product.repository.DailyPriceRecordRepository;
import com.example.budgetwise.product.repository.PriceReportRepository;
import com.example.budgetwise.product.repository.ProductInfoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class DataSeeder implements CommandLineRunner {

    private final MarketLocationRepository marketRepository;
    private final ProductInfoRepository productRepository;
    private final PriceReportRepository reportRepository;
    private final DailyPriceRecordRepository recordRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // CONFIGURATION: Number of months of historical data to generate
    private static final int MONTHS_TO_SEED = 5;

    public DataSeeder(MarketLocationRepository marketRepository,
                      ProductInfoRepository productRepository,
                      PriceReportRepository reportRepository,
                      DailyPriceRecordRepository recordRepository) {
        this.marketRepository = marketRepository;
        this.productRepository = productRepository;
        this.reportRepository = reportRepository;
        this.recordRepository = recordRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("Initializing Data Seeder...");

        // 1. Initialize Markets and Products if empty
        SourceData sourceData = objectMapper.readValue(RAW_JSON_DATA, SourceData.class);
        seedMarketsAndProducts(sourceData);

        LocalDate today = LocalDate.now();
        LocalDate targetStartDate = today.minusMonths(MONTHS_TO_SEED);

        // === FIX: FORCE RESET IF DATA IS INSUFFICIENT ===
        // Check if we have enough historical data (approx 120 days for 4 months)
        long currentRecordCount = reportRepository.count();

        if (currentRecordCount > 0 && currentRecordCount < 120) {
            System.out.println("⚠️ Insufficient historical data detected (" + currentRecordCount + " days only).");
            System.out.println("⚠️ PURGING existing price data to regenerate full 5-month trend...");

            // Delete all records to ensure a clean, consistent linear trend
            recordRepository.deleteAll();
            reportRepository.deleteAll();

            System.out.println("✅ Purge complete. Starting fresh seeding...");
        }

        // 2. Determine Start Date
        // If DB is empty (after purge), start from 5 months ago.
        // If DB has data (more than 120 days), start from the last record.
        LocalDate lastDateInDb = reportRepository.findLatestReportDate().orElse(targetStartDate.minusDays(1));

        if (!lastDateInDb.isBefore(today)) {
            System.out.println("Database is already fully updated up to " + today + ". Skipping Seeder.");
            return;
        }

        // Start seeding from the day AFTER the last recorded date
        LocalDate startDate = lastDateInDb.plusDays(1);

        System.out.println("Seeding history from " + startDate + " to " + today);

        // ... (RETAIN THE REST OF THE CODE BELOW - NO CHANGES NEEDED) ...
        // ... Pre-fetch entities ...
        List<MarketLocation> allMarkets = marketRepository.findAll();
        Map<String, ProductInfo> productMap = productRepository.findAll().stream()
                .collect(Collectors.toMap(p -> p.getProductName() + "|" + p.getCategory(), p -> p, (a, b) -> a));

        Random random = new Random();
        long totalDaysSpan = ChronoUnit.DAYS.between(targetStartDate, today);
        if (totalDaysSpan == 0) totalDaysSpan = 1;

        for (LocalDate date = startDate; !date.isAfter(today); date = date.plusDays(1)) {
            // ... (SAME LOOP CODE AS BEFORE) ...
            PriceReport report = new PriceReport();
            report.setDateReported(date);
            report.setDateProcessed(date.atStartOfDay());
            report.setStatus(PriceReport.Status.COMPLETED);
            report.setUrl("https://da.gov.ph/price-watch/" + date);
            reportRepository.save(report);

            List<DailyPriceRecord> batchRecords = new ArrayList<>();

            long daysPassed = ChronoUnit.DAYS.between(targetStartDate, date);
            double timeProgress = (double) daysPassed / totalDaysSpan;

            for (MarketLocation market : allMarkets) {
                if (random.nextDouble() < 0.15) continue;

                for (PriceItem item : sourceData.price_data) {
                    ProductInfo product = productMap.get(item.commodity + "|" + item.category);
                    if (product == null) continue;

                    double trendFactor = 0.90 + (0.10 * timeProgress);
                    double marketMarkup = (market.getType() == MarketLocation.Type.SUPERMARKET) ? 1.15 : 1.0;
                    double volatility = (random.nextDouble() * 0.06) - 0.03;

                    double basePrice = item.price * trendFactor;
                    double finalPrice = basePrice * marketMarkup * (1.0 + volatility);

                    DailyPriceRecord record = new DailyPriceRecord();
                    record.setPrice(Math.round(finalPrice * 100.0) / 100.0);
                    record.setUnit(item.unit);
                    record.setOrigin(item.origin);
                    record.setMarketLocation(market);
                    record.setProductInfo(product);
                    record.setPriceReport(report);

                    batchRecords.add(record);
                }
            }
            recordRepository.saveAll(batchRecords);

            if (date.getDayOfMonth() == 1 || date.getDayOfMonth() == 15) {
                System.out.println(">> Seeded: " + date + " (Progress: " + String.format("%.0f", timeProgress * 100) + "%)");
            }
        }
        System.out.println("DATA SEEDING COMPLETE! Historical trend data is now available.");
    }
    private void seedMarketsAndProducts(SourceData sourceData) {
        if (marketRepository.count() == 0) {
            for (String marketName : sourceData.covered_markets) {
                MarketLocation m = new MarketLocation();
                m.setMarketLocation(marketName);
                m.setType(marketName.contains("Supermarket") ? MarketLocation.Type.SUPERMARKET : MarketLocation.Type.WET_MARKET);
                m.setStatus(MarketLocation.Status.ACTIVE);
                m.setRatings(4.0 + new Random().nextDouble());
                marketRepository.save(m);
            }
        }

        if (productRepository.count() == 0) {
            Set<String> seededProducts = new HashSet<>();

            for (PriceItem item : sourceData.price_data) {
                String uniqueKey = item.commodity + "|" + item.category;

                if (!seededProducts.contains(uniqueKey)) {
                    ProductInfo p = new ProductInfo();
                    p.setProductName(item.commodity);
                    p.setCategory(item.category);
                    p.setStatus(ProductInfo.Status.ACTIVE);
                    productRepository.save(p);

                    seededProducts.add(uniqueKey);
                }
            }
        }
    }

    // DTOs for JSON Mapping
    private static class SourceData {
        public String status;
        public String date_processed;
        public List<String> covered_markets;
        public List<PriceItem> price_data;
    }

    private static class PriceItem {
        public String category;
        public String commodity;
        public String origin;
        public String unit;
        public double price;
    }

    // JSON Data Source
    private static final String RAW_JSON_DATA = """
    {
        "status": "Success",
        "date_processed": "2025-12-20",
        "covered_markets": [
            "Agora Public Market",
            "Alabang Central Market (Muntinlupa)",
            "Balintawak Cloverleaf Market",
            "Bicutan Market",
            "Cartimar Market",
            "Commonwealth Market",
            "Farmers Market",
            "Guadalupe Commercial Complex",
            "Kamuning Public Market",
            "New Las Piñas City Public Market",
            "Malabon Central Market",
            "Mandaluyong Public Market",
            "Marikina Public Market",
            "Maypajo Market",
            "Mega Q-Mart",
            "Muñoz Market",
            "Murphy Public Market",
            "Navotas Agora Complex",
            "New Marulas Public Market",
            "Obrero Market",
            "Pasay City Public Market",
            "Mutya ng Pasig Mega Market",
            "Pritil Market",
            "Quinta Market",
            "San Andres Market",
            "Taguig People's Market",
            "Trabajo Market"
        ],
        "price_data": [
            { "category": "COMMERCIAL RICE", "commodity": "Basmati Rice", "origin": "Imported", "unit": "kg", "price": 211.0 },
            { "category": "COMMERCIAL RICE", "commodity": "Glutinous Rice", "origin": "Imported", "unit": "kg", "price": 61.43 },
            { "category": "COMMERCIAL RICE", "commodity": "Jasponica Rice", "origin": "Imported", "unit": "kg", "price": 61.48 },
            { "category": "COMMERCIAL RICE", "commodity": "Special White Rice", "origin": "Imported", "unit": "kg", "price": 59.52 },
            { "category": "COMMERCIAL RICE", "commodity": "Premium Rice", "origin": "Imported", "unit": "kg", "price": 50.42 },
            { "category": "COMMERCIAL RICE", "commodity": "Well Milled Rice", "origin": "Imported", "unit": "kg", "price": 44.54 },
            { "category": "COMMERCIAL RICE", "commodity": "Regular Milled Rice", "origin": "Imported", "unit": "kg", "price": 39.38 },
            { "category": "COMMERCIAL RICE", "commodity": "Glutinous Rice", "origin": "Local", "unit": "kg", "price": 77.53 },
            { "category": "COMMERCIAL RICE", "commodity": "Special White Rice", "origin": "Local", "unit": "kg", "price": 57.73 },
            { "category": "COMMERCIAL RICE", "commodity": "Premium Rice", "origin": "Local", "unit": "kg", "price": 50.59 },
            { "category": "COMMERCIAL RICE", "commodity": "Well Milled Rice", "origin": "Local", "unit": "kg", "price": 43.84 },
            { "category": "COMMERCIAL RICE", "commodity": "Regular Milled Rice", "origin": "Local", "unit": "kg", "price": 38.86 },
            { "category": "CORN PRODUCTS", "commodity": "Corn White", "origin": "Local", "unit": "kg", "price": 108.13 },
            { "category": "CORN PRODUCTS", "commodity": "Corn Yellow", "origin": "Local", "unit": "kg", "price": 89.83 },
            { "category": "CORN PRODUCTS", "commodity": "Corn Grits White", "origin": "Local", "unit": "kg", "price": 120.0 },
            { "category": "CORN PRODUCTS", "commodity": "Corn Grits Yellow", "origin": "Local", "unit": "kg", "price": 120.0 },
            { "category": "CORN PRODUCTS", "commodity": "Corn Cracked", "origin": "Local", "unit": "kg", "price": 50.0 },
            { "category": "CORN PRODUCTS", "commodity": "Corn Grits", "origin": "Local", "unit": "kg", "price": 46.67 },
            { "category": "FISH PRODUCTS", "commodity": "Alumahan (Indian Mackerel)", "origin": "Local", "unit": "kg", "price": 361.72 },
            { "category": "FISH PRODUCTS", "commodity": "Bangus Large", "origin": "Local", "unit": "kg", "price": 265.06 },
            { "category": "FISH PRODUCTS", "commodity": "Bangus Medium", "origin": "Local", "unit": "kg", "price": 238.65 },
            { "category": "FISH PRODUCTS", "commodity": "Galunggong", "origin": "Local", "unit": "kg", "price": 357.2 },
            { "category": "FISH PRODUCTS", "commodity": "Galunggong", "origin": "Imported", "unit": "kg", "price": 310.77 },
            { "category": "FISH PRODUCTS", "commodity": "Pampano", "origin": "Local", "unit": "kg", "price": 508.13 },
            { "category": "FISH PRODUCTS", "commodity": "Pampano", "origin": "Imported", "unit": "kg", "price": 407.5 },
            { "category": "FISH PRODUCTS", "commodity": "Salmon Belly", "origin": "Imported", "unit": "kg", "price": 418.15 },
            { "category": "FISH PRODUCTS", "commodity": "Salmon Head", "origin": "Imported", "unit": "kg", "price": 224.8 },
            { "category": "FISH PRODUCTS", "commodity": "Sardines (Tamban)", "origin": "Local", "unit": "kg", "price": 147.27 },
            { "category": "FISH PRODUCTS", "commodity": "Squid", "origin": "Local", "unit": "kg", "price": 471.48 },
            { "category": "FISH PRODUCTS", "commodity": "Squid", "origin": "Imported", "unit": "kg", "price": 217.69 },
            { "category": "FISH PRODUCTS", "commodity": "Tambakol (Yellow-Fin Tuna)", "origin": "Local", "unit": "kg", "price": 306.61 },
            { "category": "FISH PRODUCTS", "commodity": "Tilapia", "origin": "Local", "unit": "kg", "price": 154.42 },
            { "category": "BEEF MEAT PRODUCTS", "commodity": "Beef Brisket", "origin": "Local", "unit": "kg", "price": 422.3 },
            { "category": "BEEF MEAT PRODUCTS", "commodity": "Beef Brisket", "origin": "Imported", "unit": "kg", "price": 380.0 },
            { "category": "BEEF MEAT PRODUCTS", "commodity": "Beef Chuck", "origin": "Local", "unit": "kg", "price": 414.86 },
            { "category": "BEEF MEAT PRODUCTS", "commodity": "Beef Forequarter", "origin": "Local", "unit": "kg", "price": 490.0 },
            { "category": "BEEF MEAT PRODUCTS", "commodity": "Beef Fore Limb", "origin": "Local", "unit": "kg", "price": 460.0 },
            { "category": "BEEF MEAT PRODUCTS", "commodity": "Beef Flank", "origin": "Local", "unit": "kg", "price": 461.0 },
            { "category": "BEEF MEAT PRODUCTS", "commodity": "Beef Flank", "origin": "Imported", "unit": "kg", "price": 376.67 },
            { "category": "BEEF MEAT PRODUCTS", "commodity": "Beef Loin", "origin": "Local", "unit": "kg", "price": 584.29 },
            { "category": "BEEF MEAT PRODUCTS", "commodity": "Beef Rib Eye", "origin": "Local", "unit": "kg", "price": 430.0 },
            { "category": "BEEF MEAT PRODUCTS", "commodity": "Beef Rib Set", "origin": "Local", "unit": "kg", "price": 409.69 },
            { "category": "BEEF MEAT PRODUCTS", "commodity": "Beef Rump", "origin": "Local", "unit": "kg", "price": 479.16 },
            { "category": "BEEF MEAT PRODUCTS", "commodity": "Beef Rump", "origin": "Imported", "unit": "kg", "price": 375.0 },
            { "category": "BEEF MEAT PRODUCTS", "commodity": "Beef Short Ribs", "origin": "Local", "unit": "kg", "price": 422.25 },
            { "category": "BEEF MEAT PRODUCTS", "commodity": "Beef Sirloin", "origin": "Local", "unit": "kg", "price": 479.04 },
            { "category": "BEEF MEAT PRODUCTS", "commodity": "Beef Striploin", "origin": "Local", "unit": "kg", "price": 473.2 },
            { "category": "BEEF MEAT PRODUCTS", "commodity": "Beef Tenderloin", "origin": "Local", "unit": "kg", "price": 665.0 },
            { "category": "BEEF MEAT PRODUCTS", "commodity": "Beef Tenderloin", "origin": "Imported", "unit": "kg", "price": 400.0 },
            { "category": "BEEF MEAT PRODUCTS", "commodity": "Beef Tongue", "origin": "Local", "unit": "kg", "price": 582.5 },
            { "category": "PORK MEAT PRODUCTS", "commodity": "Pork Belly (Liempo)", "origin": "Local", "unit": "kg", "price": 394.77 },
            { "category": "PORK MEAT PRODUCTS", "commodity": "Pork Belly (Liempo)", "origin": "Imported", "unit": "kg", "price": 311.83 },
            { "category": "PORK MEAT PRODUCTS", "commodity": "Pork Boston Shoulder", "origin": "Local", "unit": "kg", "price": 357.65 },
            { "category": "PORK MEAT PRODUCTS", "commodity": "Pork Chop", "origin": "Local", "unit": "kg", "price": 344.88 },
            { "category": "PORK MEAT PRODUCTS", "commodity": "Pork Chop", "origin": "Imported", "unit": "kg", "price": 254.23 },
            { "category": "PORK MEAT PRODUCTS", "commodity": "Pork Fore Shank", "origin": "Local", "unit": "kg", "price": 322.65 },
            { "category": "PORK MEAT PRODUCTS", "commodity": "Pork Fore Shank", "origin": "Imported", "unit": "kg", "price": 202.5 },
            { "category": "PORK MEAT PRODUCTS", "commodity": "Pork Head", "origin": "Local", "unit": "kg", "price": 251.05 },
            { "category": "PORK MEAT PRODUCTS", "commodity": "Pork Head", "origin": "Imported", "unit": "kg", "price": 200.0 },
            { "category": "PORK MEAT PRODUCTS", "commodity": "Pork Hind Leg (Pigue)", "origin": "Local", "unit": "kg", "price": 343.32 },
            { "category": "PORK MEAT PRODUCTS", "commodity": "Pork Hind Leg (Pigue)", "origin": "Imported", "unit": "kg", "price": 257.78 },
            { "category": "PORK MEAT PRODUCTS", "commodity": "Pork Loin", "origin": "Local", "unit": "kg", "price": 389.41 },
            { "category": "PORK MEAT PRODUCTS", "commodity": "Pork Loin", "origin": "Imported", "unit": "kg", "price": 240.0 },
            { "category": "PORK MEAT PRODUCTS", "commodity": "Pork Offals", "origin": "Local", "unit": "kg", "price": 254.32 },
            { "category": "PORK MEAT PRODUCTS", "commodity": "Pork Offals", "origin": "Imported", "unit": "kg", "price": 141.67 },
            { "category": "PORK MEAT PRODUCTS", "commodity": "Pork Picnic Shoulder (Kasim)", "origin": "Local", "unit": "kg", "price": 342.44 },
            { "category": "PORK MEAT PRODUCTS", "commodity": "Pork Picnic Shoulder (Kasim)", "origin": "Imported", "unit": "kg", "price": 255.76 },
            { "category": "PORK MEAT PRODUCTS", "commodity": "Pork Rind/Skin", "origin": "Local", "unit": "kg", "price": 107.27 },
            { "category": "PORK MEAT PRODUCTS", "commodity": "Pork Spare Ribs", "origin": "Local", "unit": "kg", "price": 336.62 },
            { "category": "PORK MEAT PRODUCTS", "commodity": "Pork Spare Ribs", "origin": "Imported", "unit": "kg", "price": 240.0 },
            { "category": "PORK MEAT PRODUCTS", "commodity": "OTHER LIVESTOCK MEAT", "origin": "Local", "unit": "kg", "price": 320.0 },
            { "category": "PORK MEAT PRODUCTS", "commodity": "Carabeef Meat", "origin": "Local", "unit": "kg", "price": 356.88 },
            { "category": "PORK MEAT PRODUCTS", "commodity": "Carabeef Rump Steak", "origin": "Local", "unit": "kg", "price": 388.33 },
            { "category": "PORK MEAT PRODUCTS", "commodity": "Carabeef Trimmings", "origin": "Local", "unit": "kg", "price": 360.0 },
            { "category": "POULTRY PRODUCTS", "commodity": "Chicken Breast", "origin": "Local", "unit": "kg", "price": 223.26 },
            { "category": "POULTRY PRODUCTS", "commodity": "Chicken Drumstick", "origin": "Local", "unit": "kg", "price": 225.48 },
            { "category": "POULTRY PRODUCTS", "commodity": "Chicken Feet", "origin": "Local", "unit": "kg", "price": 150.47 },
            { "category": "POULTRY PRODUCTS", "commodity": "Chicken Leg Quarter", "origin": "Local", "unit": "kg", "price": 225.48 },
            { "category": "POULTRY PRODUCTS", "commodity": "Chicken Leg Quarter", "origin": "Imported", "unit": "kg", "price": 170.0 },
            { "category": "POULTRY PRODUCTS", "commodity": "Chicken Liver", "origin": "Local", "unit": "kg", "price": 235.29 },
            { "category": "POULTRY PRODUCTS", "commodity": "Chicken Neck", "origin": "Local", "unit": "kg", "price": 143.56 },
            { "category": "POULTRY PRODUCTS", "commodity": "Chicken Rind/Skin", "origin": "Local", "unit": "kg", "price": 149.2 },
            { "category": "POULTRY PRODUCTS", "commodity": "Chicken Thigh", "origin": "Local", "unit": "kg", "price": 218.54 },
            { "category": "POULTRY PRODUCTS", "commodity": "Chicken Wing", "origin": "Local", "unit": "kg", "price": 226.86 },
            { "category": "POULTRY PRODUCTS", "commodity": "Whole Chicken", "origin": "Local", "unit": "kg", "price": 212.4 },
            { "category": "POULTRY PRODUCTS", "commodity": "Chicken Egg", "origin": "Local", "unit": "pc", "price": 8.28 },
            { "category": "LOWLAND VEGETABLES", "commodity": "Ampalaya/kg", "origin": "Local", "unit": "kg", "price": 138.76 },
            { "category": "LOWLAND VEGETABLES", "commodity": "Chilli (Green) Haba/Panigang", "origin": "Local", "unit": "kg", "price": 393.57 },
            { "category": "LOWLAND VEGETABLES", "commodity": "Pechay 3-4 Bundles", "origin": "Local", "unit": "kg", "price": 107.68 },
            { "category": "LOWLAND VEGETABLES", "commodity": "Pole Sitao 3-4 Bundles", "origin": "Local", "unit": "kg", "price": 141.3 },
            { "category": "LOWLAND VEGETABLES", "commodity": "Squash", "origin": "Local", "unit": "kg", "price": 60.12 },
            { "category": "LOWLAND VEGETABLES", "commodity": "Tomato/kg", "origin": "Local", "unit": "kg", "price": 153.94 },
            { "category": "HIGHLAND VEGETABLES", "commodity": "Bell Pepper (Green)", "origin": "Local", "unit": "kg", "price": 321.33 },
            { "category": "HIGHLAND VEGETABLES", "commodity": "Bell Pepper (Red)", "origin": "Local", "unit": "kg", "price": 253.98 },
            { "category": "HIGHLAND VEGETABLES", "commodity": "Broccoli", "origin": "Local", "unit": "kg", "price": 264.0 },
            { "category": "HIGHLAND VEGETABLES", "commodity": "Broccoli", "origin": "Imported", "unit": "kg", "price": 277.68 },
            { "category": "HIGHLAND VEGETABLES", "commodity": "Cauliflower", "origin": "Local", "unit": "kg", "price": 186.63 },
            { "category": "HIGHLAND VEGETABLES", "commodity": "Cauliflower", "origin": "Imported", "unit": "kg", "price": 193.33 },
            { "category": "HIGHLAND VEGETABLES", "commodity": "Cabbage (Rare Ball)", "origin": "Local", "unit": "kg", "price": 89.62 },
            { "category": "HIGHLAND VEGETABLES", "commodity": "Cabbage (Scorpio)", "origin": "Local", "unit": "kg", "price": 87.37 },
            { "category": "HIGHLAND VEGETABLES", "commodity": "Cabbage (Wonder Ball)", "origin": "Local", "unit": "kg", "price": 79.48 },
            { "category": "HIGHLAND VEGETABLES", "commodity": "Carrots", "origin": "Local", "unit": "kg", "price": 111.07 },
            { "category": "HIGHLAND VEGETABLES", "commodity": "Carrots", "origin": "Imported", "unit": "kg", "price": 104.57 },
            { "category": "HIGHLAND VEGETABLES", "commodity": "Celery", "origin": "Local", "unit": "kg", "price": 179.88 },
            { "category": "HIGHLAND VEGETABLES", "commodity": "Chayote", "origin": "Local", "unit": "kg", "price": 114.65 },
            { "category": "HIGHLAND VEGETABLES", "commodity": "Baguio Beans", "origin": "Local", "unit": "kg", "price": 193.19 },
            { "category": "HIGHLAND VEGETABLES", "commodity": "Pechay Baguio", "origin": "Local", "unit": "kg", "price": 94.45 },
            { "category": "HIGHLAND VEGETABLES", "commodity": "Lettuce (Green Ice)", "origin": "Local", "unit": "kg", "price": 371.88 },
            { "category": "HIGHLAND VEGETABLES", "commodity": "Lettuce (Iceberg)", "origin": "Local", "unit": "kg", "price": 438.79 },
            { "category": "HIGHLAND VEGETABLES", "commodity": "Lettuce (Romaine)", "origin": "Local", "unit": "kg", "price": 396.47 },
            { "category": "HIGHLAND VEGETABLES", "commodity": "White Potato", "origin": "Local", "unit": "kg", "price": 146.14 },
            { "category": "SPICES", "commodity": "Chilli Red", "origin": "Local", "unit": "kg", "price": 605.47 },
            { "category": "SPICES", "commodity": "Garlic Native", "origin": "Local", "unit": "kg", "price": 400.0 },
            { "category": "SPICES", "commodity": "Garlic", "origin": "Imported", "unit": "kg", "price": 153.4 },
            { "category": "SPICES", "commodity": "Ginger", "origin": "Local", "unit": "kg", "price": 171.19 },
            { "category": "SPICES", "commodity": "Red Onion", "origin": "Local", "unit": "kg", "price": 222.14 },
            { "category": "SPICES", "commodity": "Red Onion", "origin": "Imported", "unit": "kg", "price": 179.11 },
            { "category": "SPICES", "commodity": "White Onion", "origin": "Imported", "unit": "kg", "price": 137.7 },
            { "category": "SPICES", "commodity": "LEGUMES", "origin": "Local", "unit": "kg", "price": 128.17 },
            { "category": "FRUITS", "commodity": "Avocado", "origin": "Local", "unit": "kg", "price": 402.0 },
            { "category": "FRUITS", "commodity": "Banana (Lakatan)", "origin": "Local", "unit": "kg", "price": 97.28 },
            { "category": "FRUITS", "commodity": "Banana (Latundan)", "origin": "Local", "unit": "kg", "price": 75.43 },
            { "category": "FRUITS", "commodity": "Banana (Saba)", "origin": "Local", "unit": "kg", "price": 53.0 },
            { "category": "FRUITS", "commodity": "Calamansi", "origin": "Local", "unit": "kg", "price": 107.86 },
            { "category": "FRUITS", "commodity": "Mango (Carabao)", "origin": "Local", "unit": "kg", "price": 213.93 },
            { "category": "FRUITS", "commodity": "Melon", "origin": "Local", "unit": "kg", "price": 107.12 },
            { "category": "FRUITS", "commodity": "Papaya", "origin": "Local", "unit": "kg", "price": 70.69 },
            { "category": "FRUITS", "commodity": "Pomelo", "origin": "Local", "unit": "kg", "price": 174.83 },
            { "category": "FRUITS", "commodity": "Watermelon", "origin": "Local", "unit": "kg", "price": 76.94 },
            { "category": "OTHER BASIC COMMODITIES", "commodity": "Salt (Rock)", "origin": "Local", "unit": "kg", "price": 21.33 },
            { "category": "OTHER BASIC COMMODITIES", "commodity": "Salt (Iodized)", "origin": "Local", "unit": "kg", "price": 39.71 },
            { "category": "OTHER BASIC COMMODITIES", "commodity": "Sugar (Refined)", "origin": "Local", "unit": "kg", "price": 81.93 },
            { "category": "OTHER BASIC COMMODITIES", "commodity": "Sugar (Brown)", "origin": "Local", "unit": "kg", "price": 73.84 },
            { "category": "OTHER BASIC COMMODITIES", "commodity": "Cooking Oil (Palm)", "origin": "Local", "unit": "350 ml", "price": 36.25 },
            { "category": "OTHER BASIC COMMODITIES", "commodity": "Cooking Oil (Palm)", "origin": "Local", "unit": "1 L", "price": 90.95 },
            { "category": "OTHER BASIC COMMODITIES", "commodity": "Cooking Oil (Coconut)", "origin": "Local", "unit": "350 ml", "price": 58.68 },
            { "category": "OTHER BASIC COMMODITIES", "commodity": "Cooking Oil (Coconut)", "origin": "Local", "unit": "1 L", "price": 162.25 },
            { "category": "OTHER BASIC COMMODITIES", "commodity": "Cooking Oil (Minola)", "origin": "Local", "unit": "500 ml", "price": 90.0 },
            { "category": "OTHER BASIC COMMODITIES", "commodity": "Cooking Oil (Minola)", "origin": "Local", "unit": "1 L", "price": 140.0 },
            { "category": "OTHER BASIC COMMODITIES", "commodity": "Cooking Oil (Spring)", "origin": "Local", "unit": "1 L", "price": 150.71 },
            { "category": "OTHER BASIC COMMODITIES", "commodity": "Cooking Oil (Palm Olein (Jolly))", "origin": "Local", "unit": "1 L", "price": 143.33 }
        ]
    }
    """;
}