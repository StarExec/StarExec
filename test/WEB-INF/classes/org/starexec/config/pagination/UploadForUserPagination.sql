/* Gets the fewest necessary Uplaods in order to service a client's request for the next page of uploads in their DataTable object by supporting filtering by a query, ordering results by a column, and sorting results in ASC or DESC order. Gets uploads across all spaces for one user.
Author: Archie Kipp
*/

                                SELECT  *

                                FROM    benchmark_uploads
                                where user_id = :userId 

                                -- Exclude uploads whose upload times doesn't contain the query string                                                                                                           
                                AND     (upload_time        LIKE    CONCAT('%', :query, '%'))
