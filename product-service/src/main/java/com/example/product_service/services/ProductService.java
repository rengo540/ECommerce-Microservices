package com.example.product_service.services;

import com.example.product_service.dtos.AddProductDto;
import com.example.product_service.dtos.OneProductDto;
import com.example.product_service.dtos.ProductDto;
import com.example.product_service.dtos.ProductUpdateRequest;
import com.example.product_service.exceptions.AlreadyExistsException;
import com.example.product_service.exceptions.ProductNotFoundException;
import com.example.product_service.exceptions.ResourceNotFoundException;
import com.example.product_service.models.Category;
import com.example.product_service.models.Product;
import com.example.product_service.repos.CategoryRepository;
import com.example.product_service.repos.ImageRepo;
import com.example.product_service.repos.ProductRepo;
import com.example.product_service.services.iservices.IProductService;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService implements IProductService {

    @Autowired
    private ProductRepo productRepository ;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ImageRepo imageRepository;
    @Autowired
    private ModelMapper modelMapper;

    @Override
    @Transactional
    public Product addProduct(AddProductDto addProductDto) {

        if(productExists(addProductDto.getName(),addProductDto.getBrand())){
            throw new AlreadyExistsException("this product already exist");
        }

        Category category = categoryRepository.findByName(addProductDto.getCategory().getName())
                .orElseGet(()->{
                    Category newCategory = new Category(addProductDto.getCategory().getName());
                    return categoryRepository.save(newCategory);
                });
        addProductDto.setCategory(category);
        return productRepository.save(this.createProduct(addProductDto,category));
    }

    private boolean productExists(String name,String brand){
        return productRepository.existsByNameAndBrand(name,brand);
    }

    private Product createProduct(AddProductDto addProductDto, Category category){
        return new Product(
                addProductDto.getName(),
                addProductDto.getBrand(),
                addProductDto.getPrice(),
                addProductDto.getInventory(),
                addProductDto.getDescription(),
                category
        );
    }

    @Override
    public Page<Product> getAllProducts(int pageNumber, int pageSize, String sortBy) {
        Pageable pageable = PageRequest.of(pageNumber,pageSize, Sort.by(Sort.Direction.ASC,sortBy));
        return productRepository.findAll(pageable);
    }

    @Override
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(()-> new ProductNotFoundException("product not found"));

    }

    @Override
    public void deleteProductById(Long id) {
        productRepository.findById(id).ifPresentOrElse(productRepository::delete,
                () -> {
                    throw new ProductNotFoundException("product not found");
                });
    }

    @Transactional
    @Override
    public Product updateProduct(ProductUpdateRequest request, Long productId) {
        return productRepository.findById(productId).
                map(existingProduct -> updateExistingProduct(existingProduct,request)).
                map(productRepository::save).
                orElseThrow(()->new ProductNotFoundException("product not found"));
    }

    private Product updateExistingProduct(Product existingProduct, ProductUpdateRequest request) {
        existingProduct.setName(request.getName());
        existingProduct.setBrand(request.getBrand());
        existingProduct.setPrice(request.getPrice());
        existingProduct.setInventory(request.getInventory());
        existingProduct.setDescription(request.getDescription());

        if(request.getCategory()==null){
            return existingProduct;
        }
        Category category = categoryRepository.findByName(request.getCategory().getName()).
                orElseThrow(() -> new ResourceNotFoundException("categroy not found"));
        existingProduct.setCategory(category);
        return  existingProduct;

    }

    @Override
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategoryName(category);
    }

    @Override
    public List<Product> getProductsByBrand(String brand) {
        return productRepository.findByBrand(brand);
    }

    @Override
    public List<Product> getProductsByCategoryAndBrand(String category, String brand) {
        return productRepository.findByCategoryNameAndBrand(category,brand);
    }

    @Override
    public List<Product> getProductsByName(String name) {
        return productRepository.findByName(name);
    }

    @Override
    public List<Product> getProductsByBrandAndName(String brand, String name) {
        return productRepository.findByBrandAndName(brand,name);
    }

    @Override
    public Long countProductsByBrandAndName(String brand, String name) {
        return productRepository.countByBrandAndName(brand,name);
    }

    @Override
    public ProductDto convertToDto(Product product) {
        ProductDto productDto = modelMapper.map(product, ProductDto.class);
//        Image image = imageRepository.findFirstByProductId(product.getId());
//        ImageDto imageDto =modelMapper.map(image, ImageDto.class);
//        productDto.setImage(imageDto);
        return productDto;
    }

    @Override
    public OneProductDto convertToOneProductDto(Product product){
        //this map calls getImages()(turn on lazey fetch) so all images were appeared
        return modelMapper.map(product, OneProductDto.class);
    }

    @Override
    public List<ProductDto> getConvertedProducts(List<Product> products) {
        return products.stream().map(this::convertToDto).toList();
    }
}
