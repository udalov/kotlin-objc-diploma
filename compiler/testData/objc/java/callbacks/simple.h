#import <Foundation/Foundation.h>

typedef void (*fun_t)(void);

@interface A : NSObject
+ (void) invoke: (fun_t) fun;
@end
